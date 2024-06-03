package org.littlewings.lucene.directory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ByteBuffersDirectoryTest {
    @Test
    void inMemory() throws IOException, ParseException {
        try (Directory directory = new ByteBuffersDirectory()) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                Document document1 = new Document();
                document1.add(new TextField("field1", "Apache Lucene", Field.Store.YES));
                writer.addDocument(document1);

                Document document2 = new Document();
                document2.add(new TextField("field1", "Elasticsearch", Field.Store.YES));
                writer.addDocument(document2);

                Document document3 = new Document();
                document3.add(new TextField("field1", "Apache Solr", Field.Store.YES));
                writer.addDocument(document3);
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                QueryParser queryParser = new QueryParser("field1", analyzer);
                Query query = queryParser.parse("field1: Apache");

                TopDocs topDocs =
                        searcher.search(query, 10);
                ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                StoredFields storedFields = searcher.storedFields();

                List<Document> resultDocuments =
                        Arrays
                                .stream(scoreDocs)
                                .map(scoreDoc -> {
                                    try {
                                        return storedFields.document(scoreDoc.doc);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                                .toList();

                assertThat(resultDocuments.get(0).getField("field1").stringValue()).isEqualTo("Apache Lucene");
                assertThat(resultDocuments.get(1).getField("field1").stringValue()).isEqualTo("Apache Solr");
            }
        }
    }
}
