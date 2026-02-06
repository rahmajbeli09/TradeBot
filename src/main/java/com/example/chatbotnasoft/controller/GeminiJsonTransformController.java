package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.service.GeminiJsonToFeedMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller pour transformer les JSON bruts de Gemini en FeedMapping
 */
@RestController
@RequestMapping("/api/gemini-transform")
@RequiredArgsConstructor
@Slf4j
public class GeminiJsonTransformController {
    
    private final GeminiJsonToFeedMappingService transformService;
    
    /**
     * Transforme un JSON brut Gemini en FeedMapping
     */
    @PostMapping("/transform")
    public ResponseEntity<?> transformGeminiJson(@RequestBody Map<String, Object> request) {
        try {
            String geminiJson = (String) request.get("geminiJson");
            String existingId = (String) request.get("existingId");
            String msgType = (String) request.get("msgType");
            String createdAt = (String) request.get("createdAt");
            
            if (geminiJson == null || geminiJson.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Le paramètre 'geminiJson' est requis"
                ));
            }
            
            // Valider le JSON
            if (!transformService.validateGeminiJson(geminiJson)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "JSON Gemini invalide : champs 'fields' et 'values' requis"
                ));
            }
            
            // Transformer
            FeedMapping feedMapping = transformService.transformGeminiJsonWithMetadata(
                geminiJson, existingId, msgType, createdAt
            );
            
            // Générer le document complet
            Map<String, Object> document = transformService.generateCompleteDocument(feedMapping);
            
            log.info("✅ Transformation JSON Gemini réussie pour msgType: {}", feedMapping.getMsgType());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "feedMapping", feedMapping,
                "document", document
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la transformation JSON: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Transforme un JSON brut simple (sans métadonnées)
     */
    @PostMapping("/transform-simple")
    public ResponseEntity<?> transformSimpleGeminiJson(@RequestBody Map<String, String> request) {
        try {
            String geminiJson = request.get("geminiJson");
            
            if (geminiJson == null || geminiJson.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Le paramètre 'geminiJson' est requis"
                ));
            }
            
            // Transformer
            FeedMapping feedMapping = transformService.transformRawGeminiJson(geminiJson);
            Map<String, Object> document = transformService.generateCompleteDocument(feedMapping);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "feedMapping", feedMapping,
                "document", document
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la transformation simple: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Transforme plusieurs lignes JSON
     */
    @PostMapping("/transform-batch")
    public ResponseEntity<?> transformBatchGeminiJson(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> jsonLines = (List<String>) request.get("jsonLines");
            
            if (jsonLines == null || jsonLines.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Le paramètre 'jsonLines' est requis"
                ));
            }
            
            // Transformer toutes les lignes
            List<FeedMapping> feedMappings = transformService.transformMultipleJsonLines(jsonLines);
            List<Map<String, Object>> documents = feedMappings.stream()
                .map(transformService::generateCompleteDocument)
                .toList();
            
            log.info("✅ Transformation batch réussie : {} documents", documents.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalProcessed", documents.size(),
                "feedMappings", feedMappings,
                "documents", documents
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la transformation batch: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Valide un JSON Gemini
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateGeminiJson(@RequestBody Map<String, String> request) {
        try {
            String geminiJson = request.get("geminiJson");
            
            if (geminiJson == null || geminiJson.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Le paramètre 'geminiJson' est requis"
                ));
            }
            
            boolean isValid = transformService.validateGeminiJson(geminiJson);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isValid", isValid,
                "message", isValid ? "JSON Gemini valide" : "JSON Gemini invalide"
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la validation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint de test avec l'exemple fourni
     */
    @GetMapping("/example")
    public ResponseEntity<?> getExampleTransformation() {
        try {
            // Exemple de JSON Gemini
            String exampleGeminiJson = """
                {
                  "fields": ["Type de message", "Code de traitement", "Référence de transaction", "Montant", "Date de la transaction", "Identifiant du commerçant"],
                  "values": ["16", "002", "ABC123", "1500", "2026-02-06", "M12345"]
                }
                """;
            
            // Transformer avec métadonnées d'exemple
            FeedMapping feedMapping = transformService.transformGeminiJsonWithMetadata(
                exampleGeminiJson, 
                "6985ff1adc6f0aab18eece55", 
                "16", 
                "2026-02-06T14:47:54.192+00:00"
            );
            
            Map<String, Object> document = transformService.generateCompleteDocument(feedMapping);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "exampleInput", exampleGeminiJson,
                "feedMapping", feedMapping,
                "document", document
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'exemple: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
