package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiLlmService {

    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;

    public String generate(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiProperties.getLlmModel() + ":generateContent?key=" + geminiProperties.getApiKey();

        Map<String, Object> request = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 150,
                        "topP", 0.8,
                        "topK", 40
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(request, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ LLM: échec génération, status: {}", response.getStatusCode());
                return null;
            }

            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.error("❌ LLM: pas de candidats retournés");
                return null;
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                log.error("❌ LLM: pas de contenu dans la réponse");
                return null;
            }

            String text = (String) parts.get(0).get("text");
            log.info("✅ LLM: réponse générée ({} caractères)", text != null ? text.length() : 0);
            return text;

        } catch (Exception e) {
            log.error("❌ LLM: erreur lors de la génération: {}", e.getMessage());
            return null;
        }
    }
}
