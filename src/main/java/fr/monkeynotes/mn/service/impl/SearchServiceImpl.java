package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.dto.DtoSearchResult;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.service.SearchService;
import fr.monkeynotes.mn.service.ViewService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    private Logger LOG = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private RepositoryTranscript repositoryTranscript;
    
    @Autowired
    private ViewService viewService;

    private StandardAnalyzer analyzer;
    private Directory memoryIndex;

    @EventListener(ApplicationReadyEvent.class)
    public void initLucene() {
        analyzer = new StandardAnalyzer();
        memoryIndex = new ByteBuffersDirectory();
        try {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writter = new IndexWriter(memoryIndex, indexWriterConfig);

            List<DtoTranscript> l = repositoryTranscript.findAll().stream()
                    .map(e -> DtoTranscript.fromEntity(e))
                    .toList();

            for (DtoTranscript dtoTranscript : l) {
                LOG.info("indexing transcript {}", dtoTranscript.getName());

                String content = viewService.getContent(dtoTranscript);

                Document document = new Document();
                document.add(new TextField("title", dtoTranscript.getName(), Field.Store.YES));
                document.add(new TextField("content", content, Field.Store.NO));
                document.add(new TextField("id", dtoTranscript.getFileId(), Field.Store.YES));
                try {
                    writter.addDocument(document);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            writter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DtoSearchResult> search(String queryString) {
        String inField = "content";

        List<DtoSearchResult> dtoSearchResults = new ArrayList<>();
        try {
            Query query = new QueryParser(inField, analyzer).parse(queryString);

            IndexReader indexReader = DirectoryReader.open(memoryIndex);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 10);

            StoredFields storedFields = searcher.storedFields();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                DtoSearchResult dtoSearchResult = new DtoSearchResult()
                        .setId(doc.get("id"))
                        .setTitle(doc.get("title"));

                dtoSearchResults.add(dtoSearchResult);
            }
            return dtoSearchResults;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}