package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service pour transformer le JSON brut de Gemini en documents FeedMapping
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiJsonToFeedMappingService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Transforme un JSON brut Gemini en FeedMapping
     * @param geminiJson JSON brut fourni par Gemini
     * @param existingId ID existant (optionnel)
     * @param msgType msgType existant (optionnel)
     * @param createdAt Date de création existante (optionnel)
     * @return FeedMapping structuré
     */
    public FeedMapping transformGeminiJsonToFeedMapping(String geminiJson, String existingId, String msgType, LocalDateTime createdAt) {
        try {
            JsonNode rootNode = objectMapper.readTree(geminiJson);
            
            // Extraire les champs et valeurs du JSON Gemini
            JsonNode fieldsNode = rootNode.get("fields");
            JsonNode valuesNode = rootNode.get("values");
            
            if (fieldsNode == null || valuesNode == null || !fieldsNode.isArray() || !valuesNode.isArray()) {
                throw new IllegalArgumentException("JSON invalide : champs 'fields' et 'values' requis");
            }
            
            if (fieldsNode.size() != valuesNode.size()) {
                log.warn("⚠️ Différence de taille entre fields ({}) et values ({})", fieldsNode.size(), valuesNode.size());
            }
            
            // Créer le mapping dynamique
            Map<String, String> mapping = createDynamicMapping(fieldsNode, valuesNode);
            
            // Créer le FeedMapping
            FeedMapping feedMapping = new FeedMapping();
            
            // Utiliser les valeurs existantes ou en générer de nouvelles
            feedMapping.setId(existingId != null ? existingId : UUID.randomUUID().toString());
            feedMapping.setMsgType(msgType != null ? msgType : extractMsgTypeFromMapping(mapping));
            feedMapping.setMapping(mapping);
            feedMapping.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
            
            log.info("✅ Transformation JSON Gemini réussie : {} champs pour msgType '{}'", mapping.size(), feedMapping.getMsgType());
            
            return feedMapping;
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la transformation JSON Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de transformation JSON", e);
        }
    }
    
    /**
     * Crée un mapping dynamique à partir des champs et valeurs
     */
    private Map<String, String> createDynamicMapping(JsonNode fieldsNode, JsonNode valuesNode) {
        Map<String, String> mapping = new HashMap<>();
        
        int maxSize = Math.max(fieldsNode.size(), valuesNode.size());
        
        for (int i = 0; i < maxSize; i++) {
            String champKey = "Champ " + (i + 1);
            String fieldValue;
            
            // Extraire la valeur du champ
            if (i < fieldsNode.size() && fieldsNode.get(i) != null) {
                fieldValue = fieldsNode.get(i).asText();
            } else {
                fieldValue = "Valeur inconnue";
            }
            
            // Vérifier si la valeur correspondante existe
            if (i < valuesNode.size() && valuesNode.get(i) != null) {
                String actualValue = valuesNode.get(i).asText();
                
                // Si la valeur du champ est "Donnée anonymisée X", utiliser la valeur réelle
                if (fieldValue.matches("Donnée anonymisée \\d+")) {
                    mapping.put(champKey, actualValue);
                } else {
                    mapping.put(champKey, fieldValue);
                }
            } else {
                mapping.put(champKey, fieldValue);
            }
        }
        
        return mapping;
    }
    
    /**
     * Extrait le msgType depuis le mapping si non fourni
     */
    private String extractMsgTypeFromMapping(Map<String, String> mapping) {
        // Chercher un champ qui pourrait contenir le msgType
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String value = entry.getValue();
            if (value != null && (value.matches("\\d+") || value.matches("[A-Z]\\d*"))) {
                return value;
            }
        }
        
        // Si pas trouvé, retourner une valeur par défaut
        return "UNKNOWN";
    }
    
    /**
     * Transforme un JSON brut sans métadonnées
     */
    public FeedMapping transformRawGeminiJson(String geminiJson) {
        return transformGeminiJsonToFeedMapping(geminiJson, null, null, null);
    }
    
    /**
     * Transforme avec métadonnées complètes
     */
    public FeedMapping transformGeminiJsonWithMetadata(String geminiJson, String existingId, String msgType, String createdAtStr) {
        LocalDateTime createdAt = null;
        if (createdAtStr != null && !createdAtStr.trim().isEmpty()) {
            try {
                // Essayer différents formats de date
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                createdAt = LocalDateTime.parse(createdAtStr, formatter);
            } catch (Exception e) {
                log.warn("⚠️ Format de date non reconnu: {}, utilisation de la date actuelle", createdAtStr);
                createdAt = LocalDateTime.now();
            }
        }
        
        return transformGeminiJsonToFeedMapping(geminiJson, existingId, msgType, createdAt);
    }
    
    /**
     * Génère le document complet au format demandé
     */
    public Map<String, Object> generateCompleteDocument(FeedMapping feedMapping) {
        Map<String, Object> document = new HashMap<>();
        
        document.put("_id", feedMapping.getId());
        document.put("msgType", feedMapping.getMsgType());
        document.put("mapping", feedMapping.getMapping());
        document.put("createdAt", feedMapping.getCreatedAt());
        document.put("_class", "com.example.chatbotnasoft.entity.FeedMapping");
        
        return document;
    }
    
    /**
     * Transforme multiple lignes JSON en plusieurs FeedMapping
     */
    public java.util.List<FeedMapping> transformMultipleJsonLines(java.util.List<String> jsonLines) {
        return jsonLines.stream()
            .map(this::transformRawGeminiJson)
            .toList();
    }
    
    /**
     * Valide la structure du JSON Gemini
     */
    public boolean validateGeminiJson(String geminiJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(geminiJson);
            return rootNode.has("fields") && rootNode.has("values") &&
                   rootNode.get("fields").isArray() && rootNode.get("values").isArray();
        } catch (Exception e) {
            log.warn("⚠️ JSON invalide: {}", e.getMessage());
            return false;
        }
    }
}
