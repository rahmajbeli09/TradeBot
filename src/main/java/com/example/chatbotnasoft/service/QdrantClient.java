package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.QdrantProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantClient {

    private final QdrantProperties qdrantProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public void ensureCollectionExists(int vectorSize) {
        String collection = qdrantProperties.getCollection();
        String url = qdrantProperties.getUrl();

        if (collection == null || collection.trim().isEmpty()) {
            throw new IllegalStateException("Qdrant collection is empty");
        }

        if (collectionExists(collection)) {
            return;
        }

        createCollection(collection, vectorSize);
    }

    public boolean collectionExists(String collection) {
        try {
            String endpoint = qdrantProperties.getUrl() + "/collections/" + collection;
            ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.warn("⚠️ Qdrant: erreur lors de la vérification de collection {}: {}", collection, e.getMessage());
            return false;
        }
    }

    public void createCollection(String collection, int vectorSize) {
        String endpoint = qdrantProperties.getUrl() + "/collections/" + collection;

        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", vectorSize,
                        "distance", qdrantProperties.getDistance()
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.put(endpoint, new HttpEntity<>(body, headers));
            log.info("✅ Qdrant: collection '{}' créée (size={}, distance={})", collection, vectorSize, qdrantProperties.getDistance());
        } catch (Exception e) {
            throw new RuntimeException("Qdrant: échec création collection: " + e.getMessage(), e);
        }
    }

    public void upsertPoints(List<Map<String, Object>> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        String endpoint = qdrantProperties.getUrl() + "/collections/" + qdrantProperties.getCollection() + "/points?wait=true";

        Map<String, Object> body = Map.of("points", points);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.put(endpoint, new HttpEntity<>(body, headers));
        } catch (Exception e) {
            throw new RuntimeException("Qdrant: échec upsert points: " + e.getMessage(), e);
        }
    }

    public JsonNode scroll(int limit) {
        String endpoint = qdrantProperties.getUrl() + "/collections/" + qdrantProperties.getCollection() + "/points/scroll";

        Map<String, Object> body = Map.of(
                "limit", limit,
                "with_payload", true,
                "with_vector", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("❌ Qdrant: échec scroll: {}", e.getMessage());
            return null;
        }
    }

    public String search(List<Double> queryVector, int limit) {
        String endpoint = qdrantProperties.getUrl() + "/collections/" + qdrantProperties.getCollection() + "/points/search";

        Map<String, Object> request = Map.of(
                "vector", queryVector,
                "limit", limit,
                "with_payload", true,
                "with_vector", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(request, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Qdrant: échec search: {}", e.getMessage());
            return null;
        }
    }

    public String scrollRaw(int limit) {
        String endpoint = qdrantProperties.getUrl() + "/collections/" + qdrantProperties.getCollection() + "/points/scroll";

        Map<String, Object> body = Map.of(
                "limit", limit,
                "with_payload", true,
                "with_vector", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Qdrant: échec scrollRaw: {}", e.getMessage());
            return null;
        }
    }
}
