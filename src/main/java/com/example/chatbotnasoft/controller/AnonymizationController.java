package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.AnonymizationResult;
import com.example.chatbotnasoft.entity.Feed;
import com.example.chatbotnasoft.service.AnonymizationService;
import com.example.chatbotnasoft.service.FeedDetectionService;
import com.example.chatbotnasoft.service.FeedParsingService;
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

@RestController
@RequestMapping("/api/anonymization")
@RequiredArgsConstructor
@Slf4j
public class AnonymizationController {

    private final AnonymizationService anonymizationService;
    private final FeedDetectionService feedDetectionService;
    private final FileReadingService fileReadingService;
    private final FeedParsingService feedParsingService;

    @PostMapping("/check-msg-type/{msgType}")
    public ResponseEntity<Map<String, Object>> checkMsgType(@PathVariable String msgType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isKnown = feedDetectionService.isMsgTypeKnown(msgType);
            
            response.put("success", true);
            response.put("msgType", msgType);
            response.put("isKnown", isKnown);
            response.put("requiresAnonymization", !isKnown);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du msg-type: {}", msgType, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/anonymize-file/{fileName}")
    public ResponseEntity<Map<String, Object>> anonymizeFile(@PathVariable String fileName) {
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
            var parsingResult = feedParsingService.parseAndGroupLines(lines);
            
            // Anonymiser
            AnonymizationResult anonymizationResult = anonymizationService.processGroups(
                    parsingResult.getGroupsByMsgType());
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("totalLines", anonymizationResult.getTotalLinesProcessed());
            response.put("anonymizedLines", anonymizationResult.getAnonymizedLinesCount());
            response.put("nonAnonymizedLines", anonymizationResult.getNonAnonymizedLinesCount());
            response.put("anonymizationRate", anonymizationResult.getAnonymizationRate());
            response.put("unknownMsgTypes", anonymizationResult.getUnknownMsgTypes());
            response.put("knownMsgTypes", anonymizationResult.getKnownMsgTypes());
            response.put("resultsByMsgType", anonymizationResult.getResultsByMsgType());
            response.put("timestamp", anonymizationResult.getAnonymizedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'anonymisation du fichier: {}", fileName, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/create-feed-type")
    public ResponseEntity<Map<String, Object>> createFeedType(@RequestParam String msgType, 
                                                          @RequestParam String description) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Feed created = feedDetectionService.createFeedType(msgType, description);
            
            response.put("success", true);
            response.put("feed", created);
            response.put("message", "Msg-type créé avec succès");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du msg-type: {}", msgType, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/list-feed-types")
    public ResponseEntity<Map<String, Object>> listFeedTypes() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Feed> activeFeeds = feedDetectionService.getAllActiveFeeds();
            
            response.put("success", true);
            response.put("feedTypes", activeFeeds);
            response.put("count", activeFeeds.size());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la liste des msg-types", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
