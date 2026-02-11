package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.service.EmbeddingIndexationService;
import com.example.chatbotnasoft.service.QdrantClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/embeddings")
@RequiredArgsConstructor
public class EmbeddingIndexationController {

    private final EmbeddingIndexationService embeddingIndexationService;
    private final QdrantClient qdrantClient;

    @PostMapping("/index")
    public ResponseEntity<EmbeddingIndexationService.IndexationResult> indexAllValidatedActive() {
        return ResponseEntity.ok(embeddingIndexationService.indexAllValidatedActive());
    }

    @GetMapping("/qdrant/scroll")
    public ResponseEntity<String> scroll(@RequestParam(defaultValue = "5") int limit) {
        String result = qdrantClient.scrollRaw(limit);
        if (result == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        Integer limit = (Integer) body.getOrDefault("limit", 5);
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<Map<String, Object>> results = embeddingIndexationService.searchByQuery(query, limit);
        return ResponseEntity.ok(results);
    }
}
