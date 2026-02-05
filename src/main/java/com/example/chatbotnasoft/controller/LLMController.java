package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.AnonymizationResult;
import com.example.chatbotnasoft.dto.LLMAnalysisResult;
import com.example.chatbotnasoft.service.AnonymizationService;
import com.example.chatbotnasoft.service.FileReadingService;
import com.example.chatbotnasoft.service.LLMService;
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

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Slf4j
public class LLMController {

    private final LLMService llmService;
    private final AnonymizationService anonymizationService;
    private final FileReadingService fileReadingService;

    @PostMapping("/analyze-file/{fileName}")
    public ResponseEntity<Map<String, Object>> analyzeFile(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = Paths.get("input/feeds").resolve(fileName);
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "Fichier non trouvé: " + fileName);
                return ResponseEntity.notFound().build();
            }

            // Lire et parser le fichier
            var lines = fileReadingService.readFileLines(filePath).toList();
            var parsingResult = new com.example.chatbotnasoft.service.FeedParsingService().parseAndGroupLines(lines);
            
            // Anonymiser
            AnonymizationResult anonymizationResult = anonymizationService.processGroups(
                    parsingResult.getGroupsByMsgType());
            
            // Analyser avec LLM (uniquement les msg-types inconnus)
            Map<String, List<com.example.chatbotnasoft.dto.AnonymizedLine>> unknownLines = 
                    anonymizationResult.getResultsByMsgType().entrySet().stream()
                            .filter(entry -> anonymizationResult.getUnknownMsgTypes().contains(entry.getKey()))
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue
                            ));
            
            LLMAnalysisResult analysisResult = llmService.analyzeAnonymizedLines(unknownLines, fileName);
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("totalLines", parsingResult.getTotalLinesProcessed());
            response.put("unknownMsgTypes", anonymizationResult.getUnknownMsgTypes());
            response.put("knownMsgTypes", anonymizationResult.getKnownMsgTypes());
            response.put("analyzedLines", analysisResult.getTotalLinesAnalyzed());
            response.put("successfulAnalyses", analysisResult.getSuccessfulAnalyses());
            response.put("failedAnalyses", analysisResult.getFailedAnalyses());
            response.put("successRate", analysisResult.getSuccessRate());
            response.put("fieldMappings", analysisResult.getResultsByMsgType());
            response.put("errors", analysisResult.getAnalysisErrors());
            response.put("timestamp", analysisResult.getAnalyzedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'analyse LLM du fichier: {}", fileName, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/analyze-line")
    public ResponseEntity<Map<String, Object>> analyzeLine(@RequestParam String anonymizedLine, 
                                                     @RequestParam String msgType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Créer une ligne anonymisée temporaire
            com.example.chatbotnasoft.dto.AnonymizedLine line = new com.example.chatbotnasoft.dto.AnonymizedLine(
                    anonymizedLine, anonymizedLine, msgType, 1, "test.txt", true);
            
            Map<String, List<com.example.chatbotnasoft.dto.AnonymizedLine>> linesByMsgType = 
                    Map.of(msgType, List.of(line));
            
            LLMAnalysisResult analysisResult = llmService.analyzeAnonymizedLines(linesByMsgType, "test");
            
            response.put("success", true);
            response.put("anonymizedLine", anonymizedLine);
            response.put("msgType", msgType);
            response.put("fieldMapping", analysisResult.getResultsByMsgType().get(msgType));
            response.put("timestamp", analysisResult.getAnalyzedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'analyse LLM de la ligne", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test simple avec une ligne
            String testLine = "077;99;23012025;xxxxx;xxxxx;xxxxx";
            Map<String, List<com.example.chatbotnasoft.dto.AnonymizedLine>> testLines = 
                    Map.of("99", List.of(new com.example.chatbotnasoft.dto.AnonymizedLine(
                            testLine, testLine, "99", 1, "test.txt", true)));
            
            LLMAnalysisResult result = llmService.analyzeAnonymizedLines(testLines, "test");
            
            response.put("success", true);
            response.put("message", "Connexion Gemini OK");
            response.put("testSuccessful", result.isValid());
            response.put("analyzedLines", result.getTotalLinesAnalyzed());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du test de connexion Gemini", e);
            response.put("success", false);
            response.put("message", "Erreur de connexion: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
