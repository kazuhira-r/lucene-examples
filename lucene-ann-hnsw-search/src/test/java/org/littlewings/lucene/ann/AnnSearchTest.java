package org.littlewings.lucene.ann;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.lucene99.Lucene99Codec;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;
import org.apache.lucene.codecs.perfield.PerFieldKnnVectorsFormat;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMScripts;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.WithByteman;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WithByteman
@BMUnitConfig(loadDirectory = "target/test-classes")
@BMScripts(scripts = {
        @BMScript("trace-ann-searcher.btm"),
        @BMScript("trace-hnsw-parameters.btm"),
}
)
class AnnSearchTest {
    EmbeddingClient embeddingClient;

    @BeforeEach
    void setUp() {
        embeddingClient = EmbeddingClient.create("localhost", 8000);
    }

    @AfterEach
    void tearDown() {
        embeddingClient.close();
    }

    List<Document> createDocuments() {
        return List.of(
                createDocument(
                        "The Time Machine",
                        "A man travels through time and witnesses the evolution of humanity.",
                        "H.G. Wells",
                        1895
                ),
                createDocument(
                        "Ender's Game",
                        "A young boy is trained to become a military leader in a war against an alien race.",
                        "Orson Scott Card",
                        1985
                ),
                createDocument(
                        "Brave New World",
                        "A dystopian society where people are genetically engineered and conditioned to conform to a strict social hierarchy.",
                        "Aldous Huxley",
                        1932
                ),
                createDocument(
                        "The Hitchhiker's Guide to the Galaxy",
                        "A comedic science fiction series following the misadventures of an unwitting human and his alien friend.",
                        "Douglas Adams",
                        1979
                ),
                createDocument(
                        "Dune",
                        "A desert planet is the site of political intrigue and power struggles.",
                        "Frank Herbert",
                        1965
                ),
                createDocument(
                        "Foundation",
                        "A mathematician develops a science to predict the future of humanity and works to save civilization from collapse.",
                        "Isaac Asimov",
                        1951
                ),
                createDocument(
                        "Snow Crash",
                        "A futuristic world where the internet has evolved into a virtual reality metaverse.",
                        "Neal Stephenson",
                        1992
                ),
                createDocument(
                        "Neuromancer",
                        "A hacker is hired to pull off a near-impossible hack and gets pulled into a web of intrigue.",
                        "William Gibson",
                        1984
                ),
                createDocument(
                        "The War of the Worlds",
                        "A Martian invasion of Earth throws humanity into chaos.",
                        "H.G. Wells",
                        1898
                ),
                createDocument(
                        "The Hunger Games",
                        "A dystopian society where teenagers are forced to fight to the death in a televised spectacle.",
                        "Suzanne Collins",
                        2008
                ),
                createDocument(
                        "The Andromeda Strain",
                        "A deadly virus from outer space threatens to wipe out humanity.",
                        "Michael Crichton",
                        1969
                ),
                createDocument(
                        "The Left Hand of Darkness",
                        "A human ambassador is sent to a planet where the inhabitants are genderless and can change gender at will.",
                        "Ursula K. Le Guin",
                        1969
                ),
                createDocument(
                        "The Three-Body Problem",
                        "Humans encounter an alien civilization that lives in a dying system.",
                        "Liu Cixin",
                        2008
                )
        );
    }

    Document createDocument(String name, String description, String author, int year) {
        Document document = new Document();
        document.add(new TextField("name", name, Field.Store.YES));
        document.add(new TextField("description", description, Field.Store.YES));
        document.add(new TextField("author", author, Field.Store.YES));
        document.add(new IntField("year", year, Field.Store.YES));

        float[] vector = textToVector("passage: " + description);
        document.add(new KnnFloatVectorField("description_vector", vector, VectorSimilarityFunction.EUCLIDEAN));

        return document;
    }

    float[] textToVector(String text) {
        EmbeddingClient.EmbeddingRequest request =
                new EmbeddingClient.EmbeddingRequest(
                        "intfloat/multilingual-e5-base",
                        text
                );

        EmbeddingClient.EmbeddingResponse response = embeddingClient.execute(request);

        float[] vector = new float[response.embedding().size()];
        for (int i = 0; i < response.embedding().size(); i++) {
            vector[i] = response.embedding().get(i);
        }

        return vector;
    }

    /*
    @Test
    void knnSearch() throws IOException {
        try (Directory directory = new ByteBuffersDirectory()) {
            IndexWriterConfig config = new IndexWriterConfig();

            List<Document> documents = createDocuments();
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.addDocuments(documents);
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                float[] vector = textToVector("query: alien invasion");
                KnnFloatVectorQuery query =
                        //new KnnFloatVectorQuery("description_vector", vector, documents.size());  // all documents
                        new KnnFloatVectorQuery("description_vector", vector, documents.size() - 1);  // all documents

                TopDocs topDocs = searcher.search(query, 3);

                StoredFields storedFields = searcher.storedFields();

                Map<Document, Explanation> resultDocumentsAndExplanations =
                        Arrays.stream(topDocs.scoreDocs)
                                .map(scoreDoc -> {
                                    try {
                                        Document document = storedFields.document(scoreDoc.doc);
                                        Explanation explanation = searcher.explain(query, scoreDoc.doc);
                                        return new Object[]{document, explanation};
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                                .collect(Collectors.toMap(
                                        o -> (Document) o[0],
                                        o -> (Explanation) o[1],
                                        (e1, e2) -> e1,
                                        LinkedHashMap::new
                                ));

                assertThat(resultDocumentsAndExplanations).hasSize(3);

                List<Document> resultDocuments = new ArrayList<>(resultDocumentsAndExplanations.keySet());
                assertThat(resultDocuments.get(0).getField("name").stringValue())
                        .isEqualTo("The Hitchhiker's Guide to the Galaxy");
                assertThat(resultDocuments.get(0).getField("year").numericValue().intValue())
                        .isEqualTo(1979);
                assertThat(resultDocuments.get(1).getField("name").stringValue())
                        .isEqualTo("The Three-Body Problem");
                assertThat(resultDocuments.get(1).getField("year").numericValue().intValue())
                        .isEqualTo(2008);
                assertThat(resultDocuments.get(2).getField("name").stringValue())
                        .isEqualTo("The Andromeda Strain");
                assertThat(resultDocuments.get(2).getField("year").numericValue().intValue())
                        .isEqualTo(1969);

                for (Map.Entry<Document, Explanation> entry : resultDocumentsAndExplanations.entrySet()) {
                    Document document = entry.getKey();
                    Explanation explanation = entry.getValue();

                    System.out.printf("Explanation As String(doc = %s):%n", document.getField("name").stringValue());
                    explanation.toString().lines().map(s -> "    " + s).forEach(System.out::println);
                    System.out.println(explanation.getDetails().length);
                    System.out.println("---------------");
                }
            }
        }
    }
     */

    @Test
    void knnSearch() throws IOException {
        try (Directory directory = new ByteBuffersDirectory()) {
            IndexWriterConfig config = new IndexWriterConfig();

            List<Document> documents = createDocuments();

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.addDocuments(documents);
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                int k = documents.size(); // all document

                float[] vector = textToVector("query: alien invasion");
                KnnFloatVectorQuery query =
                        new KnnFloatVectorQuery("description_vector", vector, k);

                TopDocs topDocs = searcher.search(query, 3);

                StoredFields storedFields = searcher.storedFields();

                List<Document> resultDocuments =
                        Arrays.stream(topDocs.scoreDocs)
                                .map(scoreDoc -> {
                                    try {
                                        return storedFields.document(scoreDoc.doc);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                                .toList();

                assertThat(resultDocuments).hasSize(3);
                assertThat(resultDocuments.get(0).getField("name").stringValue())
                        .isEqualTo("The Hitchhiker's Guide to the Galaxy");
                assertThat(resultDocuments.get(0).getField("year").numericValue().intValue())
                        .isEqualTo(1979);
                assertThat(resultDocuments.get(1).getField("name").stringValue())
                        .isEqualTo("The Three-Body Problem");
                assertThat(resultDocuments.get(1).getField("year").numericValue().intValue())
                        .isEqualTo(2008);
                assertThat(resultDocuments.get(2).getField("name").stringValue())
                        .isEqualTo("The Andromeda Strain");
                assertThat(resultDocuments.get(2).getField("year").numericValue().intValue())
                        .isEqualTo(1969);
            }
        }
    }

    @Test
    void annSearch() throws IOException {
        try (Directory directory = new ByteBuffersDirectory()) {
            IndexWriterConfig config = new IndexWriterConfig();

            List<Document> documents = createDocuments();

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.addDocuments(documents);
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                int k = documents.size() - 1; // all documents - 1

                float[] vector = textToVector("query: alien invasion");
                KnnFloatVectorQuery query =
                        new KnnFloatVectorQuery("description_vector", vector, k);

                TopDocs topDocs = searcher.search(query, 3);

                StoredFields storedFields = searcher.storedFields();

                List<Document> resultDocuments =
                        Arrays.stream(topDocs.scoreDocs)
                                .map(scoreDoc -> {
                                    try {
                                        return storedFields.document(scoreDoc.doc);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                                .toList();

                assertThat(resultDocuments).hasSize(3);
                assertThat(resultDocuments.get(0).getField("name").stringValue())
                        .isEqualTo("The Hitchhiker's Guide to the Galaxy");
                assertThat(resultDocuments.get(0).getField("year").numericValue().intValue())
                        .isEqualTo(1979);
                assertThat(resultDocuments.get(1).getField("name").stringValue())
                        .isEqualTo("The Three-Body Problem");
                assertThat(resultDocuments.get(1).getField("year").numericValue().intValue())
                        .isEqualTo(2008);
                assertThat(resultDocuments.get(2).getField("name").stringValue())
                        .isEqualTo("The Andromeda Strain");
                assertThat(resultDocuments.get(2).getField("year").numericValue().intValue())
                        .isEqualTo(1969);
            }
        }
    }

    @Test
    void defaultCodec() throws ReflectiveOperationException {
        Codec codec = Codec.getDefault();
        assertThat(codec).isExactlyInstanceOf(Lucene99Codec.class);
        assertThat(codec.knnVectorsFormat()).isInstanceOf(PerFieldKnnVectorsFormat.class);

        Lucene99Codec lucene99Codec = (Lucene99Codec) codec;

        java.lang.reflect.Field defaultKnnVectorsFormatField =
                Lucene99Codec.class.getDeclaredField("defaultKnnVectorsFormat");
        defaultKnnVectorsFormatField.setAccessible(true);
        KnnVectorsFormat knnVectorsFormat = (KnnVectorsFormat) defaultKnnVectorsFormatField.get(lucene99Codec);
        assertThat(knnVectorsFormat).isExactlyInstanceOf(Lucene99HnswVectorsFormat.class);
    }

    @Test
    void customizeHsnwParamersCodec1() {
        Codec codec = Codec.forName("MyCustom");  // load from ServiceLoader
        assertThat(codec).isExactlyInstanceOf(MyCustomCodec.class);
    }

    @Test
    void customizeHsnwParamersCodec2() {
        Codec codec = new MyCustomCodec();
    }

    Document createDocument2(String name, String description, String author, int year) {
        Document document = new Document();
        document.add(new TextField("name", name, Field.Store.YES));
        document.add(new TextField("description", description, Field.Store.YES));
        document.add(new TextField("author", author, Field.Store.YES));
        document.add(new IntField("year", year, Field.Store.YES));

        float[] vector = textToVector("passage: " + description);
        document.add(new KnnFloatVectorField("description_vector", vector, VectorSimilarityFunction.EUCLIDEAN));
        document.add(new KnnFloatVectorField("description_vector2", vector, VectorSimilarityFunction.EUCLIDEAN));

        return document;
    }

    @Test
    void customizeHnswParameters() throws IOException {
        try (Directory directory = new ByteBuffersDirectory()) {
            IndexWriterConfig config = new IndexWriterConfig();
            Codec codec = Codec.forName("MyCustom");
            config.setCodec(codec);

            List<Document> documents = List.of(
                    createDocument2(
                            "The Time Machine",
                            "A man travels through time and witnesses the evolution of humanity.",
                            "H.G. Wells",
                            1895
                    ),
                    createDocument2(
                            "Ender's Game",
                            "A young boy is trained to become a military leader in a war against an alien race.",
                            "Orson Scott Card",
                            1985
                    ),
                    createDocument2(
                            "Brave New World",
                            "A dystopian society where people are genetically engineered and conditioned to conform to a strict social hierarchy.",
                            "Aldous Huxley",
                            1932
                    )
            );

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.addDocuments(documents);
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                int k = 1; // ANN

                float[] vector = textToVector("query: alien invasion");
                KnnFloatVectorQuery query =
                        new KnnFloatVectorQuery("description_vector", vector, k);

                TopDocs topDocs = searcher.search(query, 1);

                StoredFields storedFields = searcher.storedFields();

                List<Document> resultDocuments =
                        Arrays.stream(topDocs.scoreDocs)
                                .map(scoreDoc -> {
                                    try {
                                        return storedFields.document(scoreDoc.doc);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                                .toList();

                assertThat(resultDocuments).hasSize(1);
                assertThat(resultDocuments.get(0).getField("name").stringValue())
                        .isEqualTo("Ender's Game");
                assertThat(resultDocuments.get(0).getField("year").numericValue().intValue())
                        .isEqualTo(1985);
            }
        }
    }
}
