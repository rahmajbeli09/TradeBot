package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.service.MappingCompletionService;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller pour transformer les mappings anonymisés en mappings complets
 */
@RestController
@RequestMapping("/api/mapping-completion")
@RequiredArgsConstructor
@Slf4j
public class MappingCompletionController {
    
    private final MappingCompletionService mappingCompletionService;
    private final FeedMappingRepository feedMappingRepository;
    
    /**
     * Transforme un mapping anonymisé spécifique
     * @param mappingId ID du mapping à transformer
     * @return Mapping complet avec vraies significations
     */
    @PostMapping("/complete/{mappingId}")
    public ResponseEntity<?> completeMapping(@PathVariable String mappingId) {
        try {
            // Récupérer le mapping anonymisé
            FeedMapping anonymizedMapping = feedMappingRepository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping non trouvé: " + mappingId));
            
            // Transformer en mapping complet
            FeedMapping completedMapping = mappingCompletionService.completeMapping(anonymizedMapping);
            
            log.info("✅ Mapping complété pour ID: {}", mappingId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "originalMapping", anonymizedMapping,
                "completedMapping", completedMapping
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la complétion du mapping {}: {}", mappingId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Transforme tous les mappings d'un msgType
     * @param msgType Type de message à transformer
     * @return Liste des mappings complétés
     */
    @PostMapping("/complete-by-msgtype/{msgType}")
    public ResponseEntity<?> completeMappingsByMsgType(@PathVariable String msgType) {
        try {
            // Récupérer tous les mappings pour ce msgType
            List<FeedMapping> anonymizedMappings = feedMappingRepository.findByMsgType(msgType)
                .map(List::of)
                .orElse(List.of());
            
            if (anonymizedMappings.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Aucun mapping trouvé pour msgType: " + msgType,
                    "completedMappings", List.of()
                ));
            }
            
            // Transformer tous les mappings
            List<FeedMapping> completedMappings = anonymizedMappings.stream()
                .map(mappingCompletionService::completeMapping)
                .toList();
            
            log.info("✅ {} mappings complétés pour msgType: {}", completedMappings.size(), msgType);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "msgType", msgType,
                "totalProcessed", completedMappings.size(),
                "completedMappings", completedMappings
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la complétion des mappings pour {}: {}", msgType, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Transforme un mapping depuis un corps JSON
     * @param mappingData Mapping anonymisé à transformer
     * @return Mapping complet
     */
    @PostMapping("/complete-from-json")
    public ResponseEntity<?> completeMappingFromJson(@RequestBody Map<String, Object> mappingData) {
        try {
            // Créer un FeedMapping à partir des données JSON
            FeedMapping anonymizedMapping = createFeedMappingFromJson(mappingData);
            
            // Transformer en mapping complet
            FeedMapping completedMapping = mappingCompletionService.completeMapping(anonymizedMapping);
            
            log.info("✅ Mapping complété depuis JSON pour msgType: {}", anonymizedMapping.getMsgType());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "originalMapping", anonymizedMapping,
                "completedMapping", completedMapping
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la complétion du mapping JSON: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Transforme tous les mappings anonymisés de la base
     * @return Liste de tous les mappings complétés
     */
    @PostMapping("/complete-all")
    public ResponseEntity<?> completeAllMappings() {
        try {
            // Récupérer tous les mappings
            List<FeedMapping> allMappings = feedMappingRepository.findAll();
            
            // Filtrer et transformer uniquement les mappings anonymisés
            List<FeedMapping> completedMappings = allMappings.stream()
                .filter(this::hasAnonymizedData)
                .map(mappingCompletionService::completeMapping)
                .toList();
            
            log.info("✅ {} mappings complétés sur {} mappings totaux", completedMappings.size(), allMappings.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalMappings", allMappings.size(),
                "anonymizedMappings", allMappings.size() - completedMappings.size(),
                "completedMappings", completedMappings.size(),
                "completedMappings", completedMappings
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la complétion de tous les mappings: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Affiche les mappings réels disponibles pour un msgType
     * @param msgType Type de message
     * @return Mappings réels disponibles
     */
    @GetMapping("/real-mappings/{msgType}")
    public ResponseEntity<?> getRealMappings(@PathVariable String msgType) {
        try {
            Map<String, String> realMappings = mappingCompletionService.getRealMappingsForMsgType(msgType);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "msgType", msgType,
                "realMappings", realMappings,
                "totalMappings", realMappings.size()
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des mappings réels pour {}: {}", msgType, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Ajoute un nouveau mapping réel dans la base interne
     * @param request Données du mapping à ajouter
     * @return Résultat de l'ajout
     */
    @PostMapping("/add-real-mapping")
    public ResponseEntity<?> addRealMapping(@RequestBody Map<String, String> request) {
        try {
            String msgType = request.get("msgType");
            String champKey = request.get("champKey");
            String signification = request.get("signification");
            
            if (msgType == null || champKey == null || signification == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Les paramètres msgType, champKey et signification sont requis"
                ));
            }
            
            mappingCompletionService.addRealMapping(msgType, champKey, signification);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mapping réel ajouté avec succès",
                "msgType", msgType,
                "champKey", champKey,
                "signification", signification
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'ajout du mapping réel: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Crée un FeedMapping à partir de données JSON
     */
    private FeedMapping createFeedMappingFromJson(Map<String, Object> mappingData) {
        FeedMapping mapping = new FeedMapping();
        
        mapping.setId((String) mappingData.get("_id"));
        mapping.setMsgType((String) mappingData.get("msgType"));
        
        // Extraire le mapping objet
        @SuppressWarnings("unchecked")
        Map<String, String> mappingObj = (Map<String, String>) mappingData.get("mapping");
        mapping.setMapping(mappingObj);
        
        // Gérer createdAt si présent
        if (mappingData.containsKey("createdAt")) {
            // Convertir en LocalDateTime si nécessaire
            mapping.setCreatedAt(java.time.LocalDateTime.now()); // Simplification
        }
        
        return mapping;
    }
    
    /**
     * Vérifie si un mapping contient des données anonymisées
     */
    private boolean hasAnonymizedData(FeedMapping mapping) {
        if (mapping.getMapping() == null) return false;
        
        return mapping.getMapping().values().stream()
            .anyMatch(value -> value != null && value.matches("Donnée anonymisée \\d+"));
    }
}
