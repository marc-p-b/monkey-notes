package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.DtoSearchResult;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
import fr.monkeynotes.mn.service.EditService;
import fr.monkeynotes.mn.service.SearchService;
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
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    public static final String TYPE_TITLE = "title";
    public static final String TYPE_CONTENT = "content";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_PAGE_NUMBER = "pageNumber";
    public static final String FIELD_CONTENT = "content";
    private Logger LOG = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private EditService editService;

    private StandardAnalyzer analyzer;
    private Directory memoryIndex;

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
        analyzer = new StandardAnalyzer();
        memoryIndex = new ByteBuffersDirectory();
        try {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writter = new IndexWriter(memoryIndex, indexWriterConfig);

            repositoryTranscript.findAll().stream()
                .map(e -> DtoTranscript.fromEntity(e))
                    .forEach(dtoTranscript -> {
                        LOG.info("indexing transcript {}", dtoTranscript.getName());
                        Document tDoc = new Document();
                        tDoc.add(new StringField(FIELD_TYPE, TYPE_TITLE, Field.Store.YES));
                        tDoc.add(new StringField(FIELD_ID, dtoTranscript.getFileId(), Field.Store.YES));
                        tDoc.add(new TextField(FIELD_TITLE, dtoTranscript.getName(), Field.Store.YES));

                        try {
                            writter.addDocument(tDoc);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        repositoryTranscriptPage.findByIdTranscriptPage_FileId(dtoTranscript.getFileId()).stream()
                            .map(e -> DtoTranscriptPage.fromEntity(e))
                            .forEach(dtoTranscriptPage -> {
                                Document cDoc = new Document();
                                cDoc.add(new StringField(FIELD_TYPE, TYPE_CONTENT, Field.Store.YES));
                                cDoc.add(new StringField(FIELD_ID, dtoTranscript.getFileId(), Field.Store.YES));
                                cDoc.add(new TextField(FIELD_TITLE, dtoTranscript.getName(), Field.Store.YES));
                                cDoc.add(new IntField(FIELD_PAGE_NUMBER, dtoTranscriptPage.getPageNumber(), Field.Store.YES));
                                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);
                                FieldType type = new FieldType(TextField.TYPE_STORED);
                                type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                                cDoc.add(new Field(FIELD_CONTENT, dtoTranscriptPage.getTranscript(), type));
                                try {
                                    writter.addDocument(cDoc);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    });

            writter.close();


        } catch (IOException e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<DtoSearchResult>> search(String queryString) {
        String []inFields = {FIELD_TITLE, FIELD_CONTENT};

        List<DtoSearchResult> dtoSearchResults = new ArrayList<>();
        try {
            Query query = new MultiFieldQueryParser(inFields, analyzer).parse(queryString);

            IndexReader indexReader = DirectoryReader.open(memoryIndex);
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
                }

                dtoSearchResults.add(dtoSearchResult);
            }

            Map<String, List<DtoSearchResult>> mapResults = dtoSearchResults.stream()
                    .collect(Collectors.groupingBy(DtoSearchResult::getTitle));
            return mapResults;
//            UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, analyzer);
//            highlighter.setHighlightPhrasesStrictly(true);
//            highlighter.setMaxLength(9999);
//
//            String[] fragments = highlighter.highlight("content", query, topDocs);
//            for (int i = 0; i < fragments.length; i++) {
//                System.out.println("Doc " + topDocs.scoreDocs[i].doc + ": " + fragments[i]);
//            }

        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}