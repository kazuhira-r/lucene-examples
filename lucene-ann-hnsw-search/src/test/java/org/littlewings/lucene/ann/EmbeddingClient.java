package org.littlewings.lucene.ann;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EmbeddingClient implements AutoCloseable {
    private String url;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    EmbeddingClient(String host, int port, HttpClient httpClient) {
        this.url = String.format("http://%s:%d/embeddings/encode", host, port);
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public static EmbeddingClient create(String host, int port) {
        return new EmbeddingClient(
                host,
                port,
                HttpClient
                        .newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build()
        );
    }

    public EmbeddingResponse execute(EmbeddingRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

            HttpResponse<String> httpResponse =
                    httpClient
                            .send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            return objectMapper.readValue(httpResponse.body(), EmbeddingResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }

    public record EmbeddingRequest(String model, String text) {
    }

    public record EmbeddingResponse(String model, List<Float> embedding, int dimension) {
    }
}
