package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagService ragService;

    @PostMapping("/ask")
    public ResponseEntity<RagService.RagResponse> ask(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        Integer limit = (Integer) body.getOrDefault("limit", 3);

        if (question == null || question.isBlank()) {
            RagService.RagResponse error = RagService.RagResponse.error("La question ne peut pas être vide");
            return ResponseEntity.badRequest().body(error);
        }

        log.info("🗣️ RAG question: {}", question);
        RagService.RagResponse response = ragService.ask(question, limit);

        if (response.success()) {
            log.info("✅ RAG answer: {} ({}ms)", response.answer(), response.metadata().totalTimeMs());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ RAG error: {}", response.error());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
