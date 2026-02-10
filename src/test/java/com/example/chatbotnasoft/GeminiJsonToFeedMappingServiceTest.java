package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitaire pour GeminiJsonToFeedMappingService
 */
@ExtendWith(MockitoExtension.class)
class GeminiJsonToFeedMappingServiceTest {
    
    private GeminiJsonToFeedMappingService transformService;
    
    @BeforeEach
    void setUp() {
        transformService = new GeminiJsonToFeedMappingService(new com.fasterxml.jackson.databind.ObjectMapper());
    }
    
    @Test
    void testTransformGeminiJson_WithExampleData() {
        // Given
        String geminiJson = """
            {
              "fields": ["Type de message", "Code de traitement", "Référence de transaction", "Montant", "Date de la transaction", "Identifiant du commerçant"],
              "values": ["16", "002", "ABC123", "1500", "2026-02-06", "M12345"]
            }
            """;
        
        // When
        FeedMapping result = transformService.transformGeminiJsonWithMetadata(
            geminiJson, 
            "6985ff1adc6f0aab18eece55", 
            "16", 
            "2026-02-06T14:47:54.192+00:00"
        );
        
        // Then
        assertNotNull(result);
        assertEquals("6985ff1adc6f0aab18eece55", result.getId());
        assertEquals("16", result.getMsgType());
        assertNotNull(result.getMapping());
        assertEquals(6, result.getMapping().size());
        
        // Vérifier le mapping
        assertEquals("Type de message", result.getMapping().get("Champ 1"));
        assertEquals("Code de traitement", result.getMapping().get("Champ 2"));
        assertEquals("Référence de transaction", result.getMapping().get("Champ 3"));
        assertEquals("Montant", result.getMapping().get("Champ 4"));
        assertEquals("Date de la transaction", result.getMapping().get("Champ 5"));
        assertEquals("Identifiant du commerçant", result.getMapping().get("Champ 6"));
    }
    
    @Test
    void testTransformGeminiJson_WithAnonymizedFields() {
        // Given
        String geminiJson = """
            {
              "fields": ["Type de message", "Donnée anonymisée 1", "Donnée anonymisée 2", "Montant"],
              "values": ["A3", "ID123", "REF456", "2500"]
            }
            """;
        
        // When
        FeedMapping result = transformService.transformRawGeminiJson(geminiJson);
        
        // Then
        assertNotNull(result);
        assertEquals("Type de message", result.getMapping().get("Champ 1"));
        assertEquals("ID123", result.getMapping().get("Champ 2")); // Remplacé par la vraie valeur
        assertEquals("REF456", result.getMapping().get("Champ 3")); // Remplacé par la vraie valeur
        assertEquals("Montant", result.getMapping().get("Champ 4"));
    }
    
    @Test
    void testTransformGeminiJson_WithMismatchedSizes() {
        // Given
        String geminiJson = """
            {
              "fields": ["Type de message", "Code de traitement", "Référence de transaction"],
              "values": ["A3", "ID123"]
            }
            """;
        
        // When
        FeedMapping result = transformService.transformRawGeminiJson(geminiJson);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.getMapping().size()); // 3 champs même si 2 valeurs
        assertEquals("Type de message", result.getMapping().get("Champ 1"));
        assertEquals("ID123", result.getMapping().get("Champ 2"));
        assertEquals("Valeur inconnue", result.getMapping().get("Champ 3")); // Pas de valeur correspondante
    }
    
    @Test
    void testTransformGeminiJson_WithoutMetadata() {
        // Given
        String geminiJson = """
            {
              "fields": ["Champ 1", "Champ 2"],
              "values": ["VALUE1", "VALUE2"]
            }
            """;
        
        // When
        FeedMapping result = transformService.transformRawGeminiJson(geminiJson);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getId()); // UUID généré
        assertNotNull(result.getMsgType()); // Extrait du mapping
        assertNotNull(result.getCreatedAt()); // Date actuelle
    }
    
    @Test
    void testValidateGeminiJson_ValidJson() {
        // Given
        String validJson = """
            {
              "fields": ["Type", "Code"],
              "values": ["A3", "123"]
            }
            """;
        
        // When
        boolean isValid = transformService.validateGeminiJson(validJson);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateGeminiJson_InvalidJson() {
        // Given
        String invalidJson = """
            {
              "champs": ["Type", "Code"],
              "valeurs": ["A3", "123"]
            }
            """;
        
        // When
        boolean isValid = transformService.validateGeminiJson(invalidJson);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testGenerateCompleteDocument() {
        // Given
        FeedMapping feedMapping = new FeedMapping();
        feedMapping.setId("test-id");
        feedMapping.setMsgType("A3");
        feedMapping.setMapping(Map.of("Champ 1", "Type de message"));
        feedMapping.setCreatedAt(java.time.LocalDateTime.of(2026, 2, 6, 14, 47, 54));
        
        // When
        var document = transformService.generateCompleteDocument(feedMapping);
        
        // Then
        assertEquals("test-id", document.get("_id"));
        assertEquals("A3", document.get("msgType"));
        assertEquals(feedMapping.getMapping(), document.get("mapping"));
        assertEquals(feedMapping.getCreatedAt(), document.get("createdAt"));
        assertEquals("com.example.chatbotnasoft.entity.FeedMapping", document.get("_class"));
    }
    
    @Test
    void testTransformMultipleJsonLines() {
        // Given
        List<String> jsonLines = List.of(
            """
                {
                  "fields": ["Type", "Code"],
                  "values": ["A3", "123"]
                }
                """,
            """
                {
                  "fields": ["Message", "Statut"],
                  "values": ["INFO", "OK"]
                }
                """
        );
        
        // When
        var results = transformService.transformMultipleJsonLines(jsonLines);
        
        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getMapping().size() == 2));
    }
}
