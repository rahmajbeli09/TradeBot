package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.entity.MappingStatus;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceErrorHandlingTest {

    @Mock
    private GeminiEmbeddingService geminiEmbeddingService;

    @Mock
    private QdrantClient qdrantClient;

    @Mock
    private SimpleLlmService simpleLlmService;

    @Mock
    private FeedMappingRepository feedMappingRepository;

    @InjectMocks
    private RagService ragService;

    private FeedMapping validMapping;

    @BeforeEach
    void setUp() {
        validMapping = new FeedMapping();
        validMapping.setMsgType("53");
        validMapping.setMapping(Map.of(
                "Champ 1", "Type de message",
                "Champ 3", "Identifiant unique",
                "Champ 4", "Identifiant secondaire"
        ));
        validMapping.setStatus(MappingStatus.VALIDE);
        validMapping.setIsActive(true);
    }

    @Test
    void testMessageInconnu_QdrantVide() {
        // Given
        when(geminiEmbeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        when(qdrantClient.search(any(), anyInt())).thenReturn("{\"result\":[]}");

        // When
        var response = ragService.ask("question inconnue", 3);

        // Then
        assertFalse(response.success());
        assertTrue(response.error().contains("Désolé, je n'ai pas d'information sur ce message"));
    }

    @Test
    void testSeuilConfiance_TropBas() {
        // Given
        when(geminiEmbeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        String lowScoreResponse = "{\"result\":[{\"id\":\"1\",\"score\":0.5,\"payload\":{\"msgType\":\"53\",\"status\":\"Validé\"}}]}";
        when(qdrantClient.search(any(), anyInt())).thenReturn(lowScoreResponse);
        when(feedMappingRepository.findByMsgTypeAndIsActive(eq("53"), eq(true))).thenReturn(validMapping);

        // When
        var response = ragService.ask("Explique msgType 53", 3);

        // Then
        assertFalse(response.success());
        assertTrue(response.error().contains("Désolé, la question est trop ambiguë"));
    }

    @Test
    void testReponseVide_LLMRetourneNull() {
        // Given
        when(geminiEmbeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        String validResponse = "{\"result\":[{\"id\":\"1\",\"score\":0.8,\"payload\":{\"msgType\":\"53\",\"status\":\"Validé\"}}]}";
        when(qdrantClient.search(any(), anyInt())).thenReturn(validResponse);
        when(feedMappingRepository.findByMsgTypeAndIsActive(eq("53"), eq(true))).thenReturn(validMapping);
        when(simpleLlmService.generate(any(), any())).thenReturn(null);

        // When
        var response = ragService.ask("Explique msgType 53", 3);

        // Then
        assertFalse(response.success());
        assertTrue(response.error().contains("Désolé, je n'ai pas pu générer de réponse fiable"));
    }

    @Test
    void testReponseIncoherente_MsgTypeNonCorrespondant() {
        // Given
        when(geminiEmbeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        String validResponse = "{\"result\":[{\"id\":\"1\",\"score\":0.8,\"payload\":{\"msgType\":\"53\",\"status\":\"Validé\"}}]}";
        when(qdrantClient.search(any(), anyInt())).thenReturn(validResponse);
        when(feedMappingRepository.findByMsgTypeAndIsActive(eq("53"), eq(true))).thenReturn(validMapping);
        when(simpleLlmService.generate(any(), any())).thenReturn("Réponse pour msgType 16"); // Incohérent

        // When
        var response = ragService.ask("Explique msgType 53", 3);

        // Then
        assertFalse(response.success());
        assertTrue(response.error().contains("Désolé, la question est ambiguë ou la réponse n'est pas fiable"));
    }

    @Test
    void testFeedCorrompu_LigneVide() {
        // Given
        FeedDetectionService service = new FeedDetectionService(null, null);

        // When
        var result = service.validateFeedLine("", 1);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Ligne vide détectée"));
    }

    @Test
    void testFeedCorrompu_FormatIncorrect() {
        // Given
        FeedDetectionService service = new FeedDetectionService(null, null);

        // When
        var result = service.validateFeedLine("53|donnée", 1);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Format incorrect"));
    }

    @Test
    void testFeedCorrompu_MsgTypeManquant() {
        // Given
        FeedDetectionService service = new FeedDetectionService(null, null);

        // When
        var result = service.validateFeedLine("|donnée1|donnée2|donnée3", 1);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("msgType manquant"));
    }

    @Test
    void testFeedValide_FormatCorrect() {
        // Given
        FeedDetectionService service = new FeedDetectionService(null, null);

        // When
        var result = service.validateFeedLine("53|donnée1|donnée2|donnée3", 1);

        // Then
        assertTrue(result.isValid());
        assertNull(result.errorMessage());
    }
}
