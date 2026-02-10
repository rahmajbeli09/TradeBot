package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiEmbeddingService {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<Double> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String model = geminiProperties.getEmbeddingModel();
        String url = String.format("%s/models/%s:embedContent?key=%s",
                geminiProperties.getBaseUrl(), model, geminiProperties.getApiKey());

        Map<String, Object> requestBody = Map.of(
                "content", Map.of(
                        "parts", List.of(
                                Map.of("text", text)
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("⚠️ Embedding Gemini: réponse invalide {} body={}", response.getStatusCode(), response.getBody());
                return null;
            }

            return extractVector(response.getBody());
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.error("❌ Embedding Gemini: HTTP {} body={}", e.getStatusCode(), body);
            return null;
        } catch (Exception e) {
            log.error("❌ Embedding Gemini: échec appel API: {}", e.getMessage());
            return null;
        }
    }

    private List<Double> extractVector(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddingNode = root.get("embedding");
            if (embeddingNode == null) {
                return null;
            }

            JsonNode values = embeddingNode.get("values");
            if (values == null || !values.isArray() || values.isEmpty()) {
                return null;
            }

            return objectMapper.convertValue(values, objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
        } catch (Exception e) {
            log.error("❌ Embedding Gemini: parsing réponse impossible: {}", e.getMessage());
            return null;
        }
    }
}
