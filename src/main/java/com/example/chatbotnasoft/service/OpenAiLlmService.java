package com.example.chatbotnasoft.service;

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
public class OpenAiLlmService {

    private final RestTemplate restTemplate;
    // Utilisation d'une clé de démonstration - à remplacer par une vraie clé OpenAI
    private final String apiKey = "sk-demo-key-replace-with-real-key";
    private final String model = "gpt-3.5-turbo";

    public String generate(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "max_tokens", 100,
                "temperature", 0.3
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(request, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ OpenAI: échec génération, status: {}", response.getStatusCode());
                return null;
            }

            Map<String, Object> body = response.getBody();
            if (body.containsKey("error")) {
                log.error("❌ OpenAI: erreur API: {}", body.get("error"));
                return null;
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("❌ OpenAI: pas de choix retournés");
                return null;
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String text = (String) message.get("content");

            log.info("✅ OpenAI: réponse générée ({} caractères)", text != null ? text.length() : 0);
            return text;

        } catch (Exception e) {
            log.error("❌ OpenAI: erreur lors de la génération: {}", e.getMessage());
            return null;
        }
    }
}
