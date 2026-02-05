package com.example.chatbotnasoft;

import com.example.chatbotnasoft.dto.AnonymizedLine;
import com.example.chatbotnasoft.dto.FieldMapping;
import com.example.chatbotnasoft.dto.LLMAnalysisResult;
import com.example.chatbotnasoft.service.LLMService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LLMServiceTest {

    @Autowired
    private LLMService llmService;

    @Test
    void testAnalyzeAnonymizedLines() {
        // Créer des lignes de test
        List<AnonymizedLine> lines99 = List.of(
                new AnonymizedLine("077;99;23012025;XXXX;YYYY;ZZZZ", "077;99;23012025;xxxxx;xxxxx;xxxxx", "99", 1, "test.txt", true),
                new AnonymizedLine("078;99;23012025;AAAA;BBBB;CCCC", "078;99;23012025;xxxxx;xxxxx;xxxxx", "99", 2, "test.txt", true)
        );

        Map<String, List<AnonymizedLine>> linesByMsgType = Map.of("99", lines99);

        // Analyser avec LLM
        LLMAnalysisResult result = llmService.analyzeAnonymizedLines(linesByMsgType, "test.txt");

        // Vérifications générales
        assertNotNull(result);
        assertEquals(2, result.getTotalLinesAnalyzed());
        assertTrue(result.getMsgTypeCount() >= 1);
        assertTrue(result.getSuccessfulAnalyses() >= 0);
        assertTrue(result.getSuccessRate() >= 0.0);

        // Vérifier la structure des résultats
        Map<String, List<FieldMapping>> results = result.getResultsByMsgType();
        if (results.containsKey("99")) {
            List<FieldMapping> mappings99 = results.get("99");
            assertNotNull(mappings99);
            
            // Vérifier que chaque mapping a la bonne structure
            mappings99.forEach(mapping -> {
                assertEquals("99", mapping.getMsgType());
                assertNotNull(mapping.getMapping());
                assertTrue(mapping.getMappingFieldCount() > 0);
                assertTrue(mapping.isValid());
            });
        }
    }

    @Test
    void testAnalyzeSingleLine() {
        AnonymizedLine line = new AnonymizedLine(
                "077;99;23012025;XXXX;YYYY;ZZZZ", 
                "077;99;23012025;xxxxx;xxxxx;xxxxx", 
                "99", 1, "test.txt", true);

        Map<String, List<AnonymizedLine>> linesByMsgType = Map.of("99", List.of(line));

        LLMAnalysisResult result = llmService.analyzeAnonymizedLines(linesByMsgType, "single_test.txt");

        assertNotNull(result);
        assertEquals(1, result.getTotalLinesAnalyzed());
    }

    @Test
    void testAnalyzeEmptyLines() {
        Map<String, List<AnonymizedLine>> emptyLines = Map.of();

        LLMAnalysisResult result = llmService.analyzeAnonymizedLines(emptyLines, "empty_test.txt");

        assertNotNull(result);
        assertEquals(0, result.getTotalLinesAnalyzed());
        assertEquals(0, result.getMsgTypeCount());
        assertEquals(0, result.getSuccessfulAnalyses());
        assertEquals(0.0, result.getSuccessRate(), 0.01);
    }

    @Test
    void testAnalyzeDifferentFieldCounts() {
        // Lignes avec différents nombres de champs
        List<AnonymizedLine> lines = List.of(
                new AnonymizedLine("077;99;field3", "077;99;xxxxx", "99", 1, "test.txt", true), // 3 champs
                new AnonymizedLine("078;99;field3;field4", "078;99;xxxxx;xxxxx", "99", 2, "test.txt", true), // 4 champs
                new AnonymizedLine("079;99;field3;field4;field5;field6", "079;99;xxxxx;xxxxx;xxxxx;xxxxx", "99", 3, "test.txt", true) // 6 champs
        );

        Map<String, List<AnonymizedLine>> linesByMsgType = Map.of("99", lines);

        LLMAnalysisResult result = llmService.analyzeAnonymizedLines(linesByMsgType, "field_count_test.txt");

        assertNotNull(result);
        assertEquals(3, result.getTotalLinesAnalyzed());

        // Vérifier que les mappings s'adaptent au nombre de champs
        Map<String, List<FieldMapping>> results = result.getResultsByMsgType();
        if (results.containsKey("99")) {
            List<FieldMapping> mappings = results.get("99");
            assertEquals(3, mappings.size());
            
            // Vérifier que chaque mapping correspond au bon nombre de champs
            assertEquals(3, mappings.get(0).getFieldCount()); // 3 champs
            assertEquals(4, mappings.get(1).getFieldCount()); // 4 champs
            assertEquals(6, mappings.get(2).getFieldCount()); // 6 champs
        }
    }
}
