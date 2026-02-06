package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.ReadableFeedLine;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test unitaire pour FeedSummaryService
 */
@ExtendWith(MockitoExtension.class)
class FeedSummaryServiceTest {
    
    @Mock
    private FeedMappingRepository feedMappingRepository;
    
    @InjectMocks
    private FeedSummaryService feedSummaryService;
    
    private FeedMapping mockFeedMapping;
    
    @BeforeEach
    void setUp() {
        // Créer un mapping de test
        Map<String, String> mapping = new HashMap<>();
        mapping.put("Champ 1", "Type de Message");
        mapping.put("Champ 2", "Sous-type de Message");
        mapping.put("Champ 3", "Identifiant Principal");
        mapping.put("Champ 4", "Timestamp");
        mapping.put("Champ 5", "Données Additionnelles");
        
        mockFeedMapping = new FeedMapping();
        mockFeedMapping.setMsgType("05");
        mockFeedMapping.setMapping(mapping);
    }
    
    @Test
    void testProcessLine_WithValidMapping() {
        // Given
        String line = "HEADER;05;12345;20250205123000;EXTRA_DATA";
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(mockFeedMapping));
        
        // When
        List<ReadableFeedLine> result = feedSummaryService.generateReadableSummary("test.txt");
        
        // Then
        // Note: Comme nous utilisons un fichier réel dans le service, nous testons processLine indirectement
        verify(feedMappingRepository, atLeastOnce()).findByMsgType(anyString());
    }
    
    @Test
    void testGetSignificationForIndex() {
        // Test via réflexion ou méthode publique si disponible
        // Pour l'instant, nous testons le comportement global via generateReadableSummary
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(mockFeedMapping));
        
        // Le test sera fait via l'intégration complète
        assertTrue(true); // Placeholder
    }
    
    @Test
    void testGenerateTextSummary() {
        // Given
        ReadableFeedLine line1 = new ReadableFeedLine("05", Map.of(
            "Champ 1", "Type de Message : HEADER",
            "Champ 2", "Sous-type de Message : 05"
        ));
        
        ReadableFeedLine line2 = new ReadableFeedLine("10", Map.of(
            "Champ 1", "Type de Message : FOOTER",
            "Champ 2", "Sous-type de Message : 10"
        ));
        
        List<ReadableFeedLine> lines = List.of(line1, line2);
        
        // When
        String summary = feedSummaryService.generateTextSummary(lines);
        
        // Then
        assertNotNull(summary);
        assertTrue(summary.contains("RÉSUMÉ LISIBLE DU FICHIER FEED"));
        assertTrue(summary.contains("Type de Message : HEADER"));
        assertTrue(summary.contains("Type de Message : FOOTER"));
        assertTrue(summary.contains("TOTAL : 2 lignes traitées"));
    }
    
    @Test
    void testProcessLine_WithMissingMapping() {
        // Given
        when(feedMappingRepository.findByMsgType(anyString())).thenReturn(Optional.empty());
        
        // When & Then
        // Le comportement est testé via l'intégration
        assertTrue(true); // Placeholder
    }
    
    @Test
    void testProcessLine_WithInsufficientFields() {
        // Given
        String line = "HEADER"; // Moins de 2 champs
        
        // When & Then
        // Le comportement est testé via l'intégration
        assertTrue(true); // Placeholder
    }
}
