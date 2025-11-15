package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.DtoSearchResult;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
import fr.monkeynotes.mn.service.EditService;
import fr.monkeynotes.mn.service.SearchService;
import fr.monkeynotes.mn.service.ViewService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
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

                        repositoryTranscriptPage.findByIdTranscriptPage_FileId(dtoTranscript.getFileId()).stream()
                            .map(e -> DtoTranscriptPage.fromEntity(e))
                            .forEach(dtoTranscriptPage -> {
                                Document document = new Document();
                                //indexing transcript
                                document.add(new TextField("id", dtoTranscript.getFileId(), Field.Store.YES));
                                document.add(new TextField("title", dtoTranscript.getName(), Field.Store.YES));
                                //indexing page (applying patches)
                                document.add(new IntField("pageNumber", dtoTranscriptPage.getPageNumber(), Field.Store.YES));
                                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);
                                FieldType type = new FieldType(TextField.TYPE_STORED);
                                type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                                document.add(new Field("content", dtoTranscriptPage.getTranscript(), type));
                                try {
                                    writter.addDocument(document);
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
        String inField = "content";

        List<DtoSearchResult> dtoSearchResults = new ArrayList<>();
        try {
            Query query = new QueryParser(inField, analyzer).parse(queryString);

            IndexReader indexReader = DirectoryReader.open(memoryIndex);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 100);

//            UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, analyzer);
//            highlighter.setHighlightPhrasesStrictly(true);
//            highlighter.setMaxLength(9999);
//
//            String[] fragments = highlighter.highlight("content", query, topDocs);
//            for (int i = 0; i < fragments.length; i++) {
//                System.out.println("Doc " + topDocs.scoreDocs[i].doc + ": " + fragments[i]);
//            }

            StoredFields storedFields = searcher.storedFields();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                DtoSearchResult dtoSearchResult = new DtoSearchResult()
                        .setId(doc.get("id"))
                        .setTitle(doc.get("title"))
                        .setPageNumber(Integer.valueOf(doc.get("pageNumber")));

                dtoSearchResults.add(dtoSearchResult);
            }

            Map<String, List<DtoSearchResult>> mapResults = dtoSearchResults.stream()
                    .collect(Collectors.groupingBy(DtoSearchResult::getTitle));
            return mapResults;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}