package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitaire pour MappingCompletionService
 */
@ExtendWith(MockitoExtension.class)
class MappingCompletionServiceTest {
    
    @Mock
    private FeedMappingRepository feedMappingRepository;
    
    @InjectMocks
    private MappingCompletionService mappingCompletionService;
    
    private FeedMapping anonymizedMapping;
    
    @BeforeEach
    void setUp() {
        // Créer un mapping anonymisé de test
        Map<String, String> anonymizedMappingData = new HashMap<>();
        anonymizedMappingData.put("Champ 1", "Numéro de séquence");
        anonymizedMappingData.put("Champ 2", "Type de message (A3)");
        anonymizedMappingData.put("Champ 3", "Donnée anonymisée 1");
        anonymizedMappingData.put("Champ 4", "Donnée anonymisée 2");
        
        anonymizedMapping = new FeedMapping();
        anonymizedMapping.setId("6985fac872518e868ac39e43");
        anonymizedMapping.setMsgType("A3");
        anonymizedMapping.setMapping(anonymizedMappingData);
        anonymizedMapping.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCompleteMapping_WithAnonymizedData() {
        // When
        FeedMapping completedMapping = mappingCompletionService.completeMapping(anonymizedMapping);
        
        // Then
        assertNotNull(completedMapping);
        assertEquals("A3", completedMapping.getMsgType());
        assertEquals(anonymizedMapping.getId(), completedMapping.getId());
        
        Map<String, String> completedMappingData = completedMapping.getMapping();
        assertEquals("Numéro de séquence", completedMappingData.get("Champ 1"));
        assertEquals("Type de message (A3)", completedMappingData.get("Champ 2"));
        assertEquals("Identifiant unique de l'opération", completedMappingData.get("Champ 3"));
        assertEquals("Identifiant de l'entité source (ex: compte, utilisateur)", completedMappingData.get("Champ 4"));
    }
    
    @Test
    void testCompleteMapping_WithoutAnonymizedData() {
        // Given
        Map<String, String> explicitMapping = new HashMap<>();
        explicitMapping.put("Champ 1", "Numéro de séquence");
        explicitMapping.put("Champ 2", "Type de message (A3)");
        explicitMapping.put("Champ 3", "Identifiant existant");
        explicitMapping.put("Champ 4", "Autre donnée explicite");
        
        FeedMapping explicitFeedMapping = new FeedMapping();
        explicitFeedMapping.setMsgType("A3");
        explicitFeedMapping.setMapping(explicitMapping);
        
        // When
        FeedMapping completedMapping = mappingCompletionService.completeMapping(explicitFeedMapping);
        
        // Then
        Map<String, String> result = completedMapping.getMapping();
        assertEquals("Numéro de séquence", result.get("Champ 1"));
        assertEquals("Type de message (A3)", result.get("Champ 2"));
        assertEquals("Identifiant existant", result.get("Champ 3"));
        assertEquals("Autre donnée explicite", result.get("Champ 4"));
    }
    
    @Test
    void testCompleteMapping_WithUnknownMsgType() {
        // Given
        anonymizedMapping.setMsgType("UNKNOWN");
        
        // When
        FeedMapping completedMapping = mappingCompletionService.completeMapping(anonymizedMapping);
        
        // Then
        Map<String, String> result = completedMapping.getMapping();
        assertEquals("Signification manquante pour UNKNOWN", result.get("Champ 3"));
        assertEquals("Signification manquante pour UNKNOWN", result.get("Champ 4"));
    }
    
    @Test
    void testCompleteMapping_WithNullMapping() {
        // Given
        FeedMapping nullMapping = new FeedMapping();
        nullMapping.setMapping(null);
        
        // When
        FeedMapping completedMapping = mappingCompletionService.completeMapping(nullMapping);
        
        // Then
        assertNull(completedMapping);
    }
    
    @Test
    void testAddRealMapping() {
        // When
        mappingCompletionService.addRealMapping("TEST", "Champ 5", "Nouvelle signification");
        
        // Then
        Map<String, String> realMappings = mappingCompletionService.getRealMappingsForMsgType("TEST");
        assertEquals("Nouvelle signification", realMappings.get("Champ 5"));
    }
    
    @Test
    void testGetRealMappingsForMsgType() {
        // When
        Map<String, String> a3Mappings = mappingCompletionService.getRealMappingsForMsgType("A3");
        
        // Then
        assertNotNull(a3Mappings);
        assertEquals("Identifiant unique de l'opération", a3Mappings.get("Champ 3"));
        assertEquals("Identifiant de l'entité source (ex: compte, utilisateur)", a3Mappings.get("Champ 4"));
    }
    
    @Test
    void testGetRealMappingsForUnknownMsgType() {
        // When
        Map<String, String> unknownMappings = mappingCompletionService.getRealMappingsForMsgType("UNKNOWN");
        
        // Then
        assertNotNull(unknownMappings);
        assertTrue(unknownMappings.isEmpty());
    }
}
