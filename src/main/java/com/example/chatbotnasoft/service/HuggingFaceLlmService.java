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
public class HuggingFaceLlmService {

    private final RestTemplate restTemplate;
    private final String apiKey = "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";
    private final String model = "facebook/bart-large-cnn"; // Modèle de génération fiable

    public String generate(String prompt) {
        String url = "https://api-inference.huggingface.co/models/" + model;

        Map<String, Object> request = Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                        "max_new_tokens", 100,
                        "temperature", 0.3,
                        "do_sample", true,
                        "top_p", 0.8,
                        "top_k", 40
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(request, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ HuggingFace: échec génération, status: {}", response.getStatusCode());
                return null;
            }

            Map<String, Object> body = response.getBody();
            if (body.containsKey("error")) {
                log.error("❌ HuggingFace: erreur API: {}", body.get("error"));
                return null;
            }

            // Pour flan-t5, la réponse est directement dans "generated_text"
            if (body.containsKey("generated_text")) {
                String text = (String) body.get("generated_text");
                log.info("✅ HuggingFace: réponse générée ({} caractères)", text != null ? text.length() : 0);
                return text;
            }

            log.error("❌ HuggingFace: format de réponse inattendu: {}", body);
            return null;

        } catch (Exception e) {
            log.error("❌ HuggingFace: erreur lors de la génération: {}", e.getMessage());
            return null;
        }
    }
}
