package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.RawFeedLine;
import com.example.chatbotnasoft.service.FeedProcessingService;
import com.example.chatbotnasoft.service.FileReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/feed-processing")
@RequiredArgsConstructor
@Slf4j
public class FeedProcessingController {

    private final FeedProcessingService feedProcessingService;
    private final FileReadingService fileReadingService;

    @PostMapping("/process-ready-files")
    public ResponseEntity<Map<String, Object>> processReadyFiles() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            feedProcessingService.processReadyFiles();
            
            response.put("success", true);
            response.put("message", "Traitement des fichiers prêts lancé");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement des fichiers prêts", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/read-file/{fileName}")
    public ResponseEntity<Map<String, Object>> readFile(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = Paths.get("input/feeds").resolve(fileName);
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "Fichier non trouvé: " + fileName);
                return ResponseEntity.notFound().build();
            }

            long lineCount = fileReadingService.countLines(filePath);
            
            // Lire les 10 premières lignes comme aperçu
            Map<String, Object> preview = new HashMap<>();
            try (Stream<RawFeedLine> lines = fileReadingService.readFileLines(filePath)) {
                List<RawFeedLine> firstLines = lines.limit(10).toList();
                preview.put("lines", firstLines);
                preview.put("count", firstLines.size());
            }
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("totalLines", lineCount);
            response.put("preview", preview);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du fichier: {}", fileName, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/count-lines/{fileName}")
    public ResponseEntity<Map<String, Object>> countLines(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = Paths.get("input/feeds").resolve(fileName);
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "Fichier non trouvé: " + fileName);
                return ResponseEntity.notFound().build();
            }

            long lineCount = fileReadingService.countLines(filePath);
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("lineCount", lineCount);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du comptage des lignes du fichier: {}", fileName, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
