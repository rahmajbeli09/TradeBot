package com.example.chatbotnasoft;

import com.example.chatbotnasoft.dto.AnonymizationResult;
import com.example.chatbotnasoft.dto.ParsedFeedGroup;
import com.example.chatbotnasoft.dto.RawFeedLine;
import com.example.chatbotnasoft.entity.Feed;
import com.example.chatbotnasoft.repository.FeedRepository;
import com.example.chatbotnasoft.service.AnonymizationService;
import com.example.chatbotnasoft.service.FeedDetectionService;
import com.example.chatbotnasoft.service.FeedParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AnonymizationServiceTest {

    @Autowired
    private AnonymizationService anonymizationService;
    
    @Autowired
    private FeedDetectionService feedDetectionService;
    
    @Autowired
    private FeedParsingService feedParsingService;
    
    @Autowired
    private FeedRepository feedRepository;

    private Map<String, ParsedFeedGroup> testGroups;

    @BeforeEach
    void setUp() {
        // Nettoyer la collection
        feedRepository.deleteAll();
        
        // Créer des msg-types de test dans la base
        Feed knownFeed1 = new Feed("20", "Type 20 connu");
        Feed knownFeed2 = new Feed("21", "Type 21 connu");
        feedRepository.save(knownFeed1);
        feedRepository.save(knownFeed2);
        
        // Créer les groupes de test
        List<RawFeedLine> lines20 = List.of(
                new RawFeedLine("077;20;23012025;XXXX;YYYY;ZZZZ", 1, "test.txt"),
                new RawFeedLine("078;20;23012025;AAAA;BBBB;CCCC", 2, "test.txt")
        );
        
        List<RawFeedLine> lines99 = List.of(
                new RawFeedLine("079;99;23012025;DDDD;EEEE;FFFF", 3, "test.txt"),
                new RawFeedLine("080;99;23012025;GGGG;HHHH;IIII", 4, "test.txt")
        );
        
        testGroups = new HashMap<>();
        testGroups.put("20", new ParsedFeedGroup("20", lines20));
        testGroups.put("99", new ParsedFeedGroup("99", lines99));
    }

    @Test
    void testProcessGroupsWithKnownAndUnknownMsgTypes() {
        AnonymizationResult result = anonymizationService.processGroups(testGroups);

        // Vérifications générales
        assertEquals(4, result.getTotalLinesProcessed());
        assertEquals(2, result.getAnonymizedLinesCount()); // Les lignes du msg-type 99
        assertEquals(2, result.getNonAnonymizedLinesCount()); // Les lignes du msg-type 20
        assertEquals(50.0, result.getAnonymizationRate(), 0.01);
        assertTrue(result.hasUnknownMsgTypes());
        assertEquals(1, result.getUnknownMsgTypesCount());
        assertEquals(1, result.getKnownMsgTypesCount());

        // Vérification des groupes
        Map<String, List<com.example.chatbotnasoft.dto.AnonymizedLine>> results = result.getResultsByMsgType();
        
        // Groupe "20" connu - pas d'anonymisation
        var group20 = results.get("20");
        assertEquals(2, group20.size());
        group20.forEach(line -> assertFalse(line.isWasAnonymized()));
        assertEquals("077;20;23012025;XXXX;YYYY;ZZZZ", group20.get(0).getOriginalLine());
        assertEquals("077;20;23012025;XXXX;YYYY;ZZZZ", group20.get(0).getAnonymizedLine());

        // Groupe "99" inconnu - anonymisation
        var group99 = results.get("99");
        assertEquals(2, group99.size());
        group99.forEach(line -> assertTrue(line.isWasAnonymized()));
        assertEquals("079;99;23012025;DDDD;EEEE;FFFF", group99.get(0).getOriginalLine());
        assertEquals("079;99;23012025;xxxxx;xxxxx;xxxxx", group99.get(0).getAnonymizedLine());
    }

    @Test
    void testAnonymizeLine() {
        // Test avec 5 champs
        String original = "077;99;23012025;XXXX;YYYY;ZZZZ";
        String expected = "077;99;23012025;xxxxx;xxxxx;xxxxx";
        
        // Utiliser la réflexion pour appeler la méthode privée
        try {
            java.lang.reflect.Method method = AnonymizationService.class.getDeclaredMethod("anonymizeLine", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(anonymizationService, original);
            
            assertEquals(expected, result);
        } catch (Exception e) {
            fail("Erreur lors de l'appel de la méthode anonymizeLine: " + e.getMessage());
        }
    }

    @Test
    void testIsMsgTypeKnown() {
        assertTrue(feedDetectionService.isMsgTypeKnown("20"));
        assertTrue(feedDetectionService.isMsgTypeKnown("21"));
        assertFalse(feedDetectionService.isMsgTypeKnown("99"));
        assertFalse(feedDetectionService.isMsgTypeKnown("unknown"));
    }

    @Test
    void testAnonymizationWithVariableFieldCount() {
        List<RawFeedLine> linesVariable = List.of(
                new RawFeedLine("077;99;field3", 1, "test.txt"), // 3 champs
                new RawFeedLine("078;99;field3;field4", 2, "test.txt"), // 4 champs
                new RawFeedLine("079;99;field3;field4;field5;field6", 3, "test.txt") // 6 champs
        );
        
        Map<String, ParsedFeedGroup> groups = Map.of("99", new ParsedFeedGroup("99", linesVariable));
        AnonymizationResult result = anonymizationService.processGroups(groups);
        
        var group99 = result.getResultsByMsgType().get("99");
        assertEquals(3, group99.size());
        
        // Vérifier que chaque ligne a le bon nombre de champs
        assertEquals("077;99;xxxxx", group99.get(0).getAnonymizedLine());
        assertEquals("078;99;xxxxx;xxxxx", group99.get(1).getAnonymizedLine());
        assertEquals("079;99;xxxxx;xxxxx;xxxxx;xxxxx", group99.get(2).getAnonymizedLine());
    }
}
