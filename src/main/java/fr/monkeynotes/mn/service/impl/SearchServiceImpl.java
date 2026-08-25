package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.DtoQuickNote;
import fr.monkeynotes.mn.data.dto.DtoSearchResult;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.entity.EntityQuickNote;
import fr.monkeynotes.mn.data.entity.EntityTranscript;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.repository.RepositoryQuickNote;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.EditService;
import fr.monkeynotes.mn.service.SearchService;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    public static final String TYPE_TITLE = "title";
    public static final String TYPE_CONTENT = "content";
    public static final String TYPE_QUICKNOTE = "quicknote";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PAGE_NUMBER = "pageNumber";
    public static final String FIELD_CONTENT = "content";
    private Logger LOG = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private RepositoryQuickNote repositoryQuickNote;

    @Autowired
    private EditService editService;

    private StandardAnalyzer analyzer;
    private Directory memoryIndex;

    //Kept open for the life of the bean rather than closed at the end of initLucene(). Quicknotes are
    //written constantly, and the only alternative was a full rebuild per write — which re-reads every
    //transcript and page from the database. Reads need no coordination: search() already opens a fresh
    //DirectoryReader per query, so it picks up each commit on its own.
    private IndexWriter indexWriter;

    private volatile boolean indexEnabled;

    @Autowired
    private Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void startUp() {

        String envIdxLcn = environment.getProperty("INDEX_LUCENE");

        if ((envIdxLcn != null && envIdxLcn.equals("true"))
                || Arrays.stream(environment.getActiveProfiles())
                .filter(p -> p.equals("index_lucene"))
                .findFirst().isPresent()) {
            LOG.info("*** Started index lucene search");
            initLucene();
        } else {
            LOG.warn("*** Lucene index disabled");
        }
    }

    public void initLucene() {
        //a re-init (GET /search/init) has to drop the previous writer first, otherwise it leaks and
        //keeps holding the now-orphaned Directory
        closeWriter();

        analyzer = new StandardAnalyzer();
        memoryIndex = new ByteBuffersDirectory();
        try {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriter = new IndexWriter(memoryIndex, indexWriterConfig);

            repositoryTranscript.findAll().stream()
                .map(e -> DtoTranscript.fromEntity(e))
                    .forEach(dtoTranscript -> {
                        LOG.info("indexing transcript {}", dtoTranscript.getName());
                        Document tDoc = new Document();
                        tDoc.add(new StringField(FIELD_TYPE, TYPE_TITLE, Field.Store.YES));
                        tDoc.add(new StringField(FIELD_ID, dtoTranscript.getFileId(), Field.Store.YES));
                        tDoc.add(new TextField(FIELD_TITLE, dtoTranscript.getTitle(), Field.Store.YES));
                        tDoc.add(new TextField(FIELD_NAME, dtoTranscript.getName(), Field.Store.YES));

                        try {
                            indexWriter.addDocument(tDoc);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        repositoryTranscriptPage.findByIdTranscriptPage_FileId(dtoTranscript.getFileId()).stream()
                            .map(e -> DtoTranscriptPage.fromEntity(e))
                            .forEach(dtoTranscriptPage -> {
                                Document cDoc = new Document();
                                cDoc.add(new StringField(FIELD_TYPE, TYPE_CONTENT, Field.Store.YES));
                                cDoc.add(new StringField(FIELD_ID, dtoTranscript.getFileId(), Field.Store.YES));
                                cDoc.add(new TextField(FIELD_NAME, dtoTranscript.getName(), Field.Store.YES));
                                cDoc.add(new TextField(FIELD_TITLE, dtoTranscript.getTitle(), Field.Store.YES));
                                cDoc.add(new IntField(FIELD_PAGE_NUMBER, dtoTranscriptPage.getPageNumber(), Field.Store.YES));
                                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);
                                FieldType type = new FieldType(TextField.TYPE_STORED);
                                type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                                cDoc.add(new Field(FIELD_CONTENT, dtoTranscriptPage.getTranscript(), type));
                                try {
                                    indexWriter.addDocument(cDoc);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    });

            repositoryQuickNote.findByDeletedAtIsNull().stream()
                    .map(DtoQuickNote::fromEntity)
                    .forEach(dtoQuickNote -> {
                        LOG.info("indexing quicknote {}", dtoQuickNote.getUuid());
                        try {
                            indexWriter.addDocument(quickNoteDocument(dtoQuickNote));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            //committed, not closed — see the indexWriter field
            indexWriter.commit();
            indexEnabled = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * One Lucene document per quicknote, which is what makes replacing it by FIELD_ID exact — unlike a
     * transcript, which spreads a title document plus one document per page across the same id.
     */
    private Document quickNoteDocument(DtoQuickNote dtoQuickNote) {
        String title = Objects.toString(dtoQuickNote.getTitle(), "");

        Document doc = new Document();
        doc.add(new StringField(FIELD_TYPE, TYPE_QUICKNOTE, Field.Store.YES));
        doc.add(new StringField(FIELD_ID, dtoQuickNote.getUuid().toString(), Field.Store.YES));
        doc.add(new TextField(FIELD_TITLE, title, Field.Store.YES));
        //a quicknote has no filename, so the title doubles as its name — otherwise a name: query,
        //which the frontend search box produces, would never match a note
        doc.add(new TextField(FIELD_NAME, title, Field.Store.YES));

        FieldType type = new FieldType(TextField.TYPE_STORED);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        doc.add(new Field(FIELD_CONTENT, Objects.toString(dtoQuickNote.getBody(), ""), type));

        return doc;
    }

    @Override
    public void indexQuickNote(DtoQuickNote dtoQuickNote) {
        if (indexEnabled == false) {
            LOG.debug("lucene index disabled, skipping quicknote {}", dtoQuickNote.getUuid());
            return;
        }
        try {
            indexWriter.updateDocument(new Term(FIELD_ID, dtoQuickNote.getUuid().toString()),
                    quickNoteDocument(dtoQuickNote));
            indexWriter.commit();
        } catch (IOException e) {
            //Deliberately swallowed: the note is already persisted, so failing the caller's request
            //would lose a write to protect a derived index. A /search/init rebuild recovers it.
            LOG.error("could not index quicknote {}", dtoQuickNote.getUuid(), e);
        }
    }

    @Override
    public void removeQuickNote(UUID uuid) {
        if (indexEnabled == false) {
            return;
        }
        try {
            indexWriter.deleteDocuments(new Term(FIELD_ID, uuid.toString()));
            indexWriter.commit();
        } catch (IOException e) {
            LOG.error("could not remove quicknote {} from the index", uuid, e);
        }
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            LOG.warn("quicknote search hit with an id that is not a uuid: {}", value);
            return Optional.empty();
        }
    }

    private void closeWriter() {
        indexEnabled = false;
        if (indexWriter == null) {
            return;
        }
        try {
            indexWriter.close();
        } catch (IOException e) {
            LOG.warn("could not close the previous index writer", e);
        }
        indexWriter = null;
    }

    @PreDestroy
    public void shutdown() {
        closeWriter();
    }

    @Autowired
    private AuthService authService;

    @Override
    public Map<String, List<DtoSearchResult>> search(String queryString) {
        String []inFields = {FIELD_TITLE, FIELD_CONTENT, FIELD_NAME};

        if (indexEnabled == false) {
            //previously this dereferenced a null analyzer and 500'd; an empty result set is a far
            //clearer answer to "you never turned the index on"
            LOG.warn("search requested but the lucene index is disabled");
            return Map.of();
        }

        List<DtoSearchResult> dtoSearchResults = new ArrayList<>();
        try {
            Query query = new MultiFieldQueryParser(inFields, analyzer).parse(queryString);

            try (IndexReader indexReader = DirectoryReader.open(memoryIndex)) {
                IndexSearcher searcher = new IndexSearcher(indexReader);
                TopDocs topDocs = searcher.search(query, 100);

                StoredFields storedFields = searcher.storedFields();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = storedFields.document(scoreDoc.doc);
                    DtoSearchResult dtoSearchResult = new DtoSearchResult()
                            .setId(doc.get(FIELD_ID))
                            .setTitle(doc.get(FIELD_TITLE));

                    switch (doc.get(FIELD_TYPE)) {
                        case TYPE_TITLE:
                            dtoSearchResult.setSrType(DtoSearchResult.SRType.title);
                            break;
                        case TYPE_CONTENT:
                            dtoSearchResult
                                    .setSrType(DtoSearchResult.SRType.content)
                                    .setPageNumber(Integer.valueOf(doc.get(FIELD_PAGE_NUMBER)));
                            break;
                        case TYPE_QUICKNOTE:
                            //no page number: a quicknote is a single body of text
                            dtoSearchResult.setSrType(DtoSearchResult.SRType.quicknote);
                            break;
                    }

                    dtoSearchResults.add(dtoSearchResult);
                }
            }

            String username = authService.getUsernameFromContext();

            //The Lucene index holds no username, so every user's documents match every query. These
            //two lookups are both the date enrichment AND the tenant isolation: a hit that doesn't
            //resolve to a row owned by this user is dropped below.
            Set<IdFile> setIds = dtoSearchResults.stream()
                    .filter(sr -> sr.getSrType() != DtoSearchResult.SRType.quicknote)
                    .map(sr -> IdFile.createIdFile(username, sr.getId()))
                    .collect(Collectors.toSet());

            Map<String, EntityTranscript> mapT = setIds.isEmpty()
                    ? Map.of()
                    : repositoryTranscript.findAllByIdFileIn(setIds).stream()
                        .collect(Collectors.toMap(t -> t.getIdFile().getFileId(), Function.identity()));

            Set<UUID> quickNoteIds = dtoSearchResults.stream()
                    .filter(sr -> sr.getSrType() == DtoSearchResult.SRType.quicknote)
                    .map(sr -> parseUuid(sr.getId()))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());

            Map<String, EntityQuickNote> mapQ = quickNoteIds.isEmpty()
                    ? Map.of()
                    : repositoryQuickNote
                        .findByIdQuickNote_UsernameAndIdQuickNote_UuidInAndDeletedAtIsNull(username, quickNoteIds)
                        .stream()
                        .collect(Collectors.toMap(q -> q.getIdQuickNote().getUuid().toString(), Function.identity()));

            //A hit that resolves to nothing is a stale index entry — the index isn't invalidated when
            //a transcript is deleted, so a deleted document keeps matching. Dereferencing the missing
            //map value used to NPE and take down the entire search response for every query, not just
            //the one stale hit. Drop it instead.
            Map<String, List<DtoSearchResult>> mapResults = dtoSearchResults.stream()
                    .map(e -> {
                        if (e.getSrType() == DtoSearchResult.SRType.quicknote) {
                            EntityQuickNote quickNote = mapQ.get(e.getId());
                            if (quickNote == null) {
                                LOG.warn("dropping stale search hit, no quicknote for id {}", e.getId());
                                return null;
                            }
                            //createdAt is a quicknote's equivalent of documented_at — it's what the
                            //feed and the result list sort on
                            return e.setDocumented_at(quickNote.getCreatedAt());
                        }

                        EntityTranscript transcript = mapT.get(e.getId());
                        if (transcript == null) {
                            LOG.warn("dropping stale search hit, no transcript for id {}", e.getId());
                            return null;
                        }
                        return e.setDocumented_at(transcript.getDocumented_at());
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(DtoSearchResult::getId));

            return mapResults;

        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}