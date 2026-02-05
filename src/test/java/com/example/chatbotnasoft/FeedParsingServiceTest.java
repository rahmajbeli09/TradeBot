package com.example.chatbotnasoft;

import com.example.chatbotnasoft.dto.ParsedFeedGroup;
import com.example.chatbotnasoft.dto.ParsingResult;
import com.example.chatbotnasoft.dto.RawFeedLine;
import com.example.chatbotnasoft.service.FeedParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FeedParsingServiceTest {

    @Autowired
    private FeedParsingService feedParsingService;

    private List<RawFeedLine> testLines;

    @BeforeEach
    void setUp() {
        testLines = List.of(
                new RawFeedLine("077;20;23012025;XXXX;YYYY;ZZZZ", 1, "test.txt"),
                new RawFeedLine("078;21;23012025;AAAA;BBBB;CCCC", 2, "test.txt"),
                new RawFeedLine("079;20;23012025;DDDD;EEEE;FFFF", 3, "test.txt"),
                new RawFeedLine("080;22;23012025;GGGG;HHHH;IIII", 4, "test.txt"),
                new RawFeedLine("081;20;23012025;JJJJ;KKKK;LLLL", 5, "test.txt"),
                new RawFeedLine("082;21;23012025;MMMM;NNNN;OOOO", 6, "test.txt"),
                new RawFeedLine("083;23;23012025;PPPP;QQQQ;RRRR", 7, "test.txt"),
                new RawFeedLine("084;20;23012025;SSSS;TTTT;UUUU", 8, "test.txt")
        );
    }

    @Test
    void testParseAndGroupLines() {
        ParsingResult result = feedParsingService.parseAndGroupLines(testLines);

        // Vérifications générales
        assertEquals(8, result.getTotalLinesProcessed());
        assertEquals(8, result.getValidLinesProcessed());
        assertEquals(0, result.getErrorLinesCount());
        assertEquals(100.0, result.getSuccessRate(), 0.01);
        assertEquals(5, result.getGroupCount());
        assertFalse(result.hasErrors());

        // Vérification des groupes
        Map<String, ParsedFeedGroup> groups = result.getGroupsByMsgType();
        
        // Groupe "20" devrait avoir 4 lignes
        ParsedFeedGroup group20 = groups.get("20");
        assertNotNull(group20);
        assertEquals("20", group20.getMsgType());
        assertEquals(4, group20.getTotalLines());
        assertEquals("test.txt", group20.getSourceFileName());

        // Groupe "21" devrait avoir 2 lignes
        ParsedFeedGroup group21 = groups.get("21");
        assertNotNull(group21);
        assertEquals("21", group21.getMsgType());
        assertEquals(2, group21.getTotalLines());

        // Groupes "22", "23" devraient avoir 1 ligne chacun
        ParsedFeedGroup group22 = groups.get("22");
        assertNotNull(group22);
        assertEquals(1, group22.getTotalLines());

        ParsedFeedGroup group23 = groups.get("23");
        assertNotNull(group23);
        assertEquals(1, group23.getTotalLines());
    }

    @Test
    void testParseWithErrors() {
        List<RawFeedLine> linesWithErrors = List.of(
                new RawFeedLine("077;20;23012025;XXXX;YYYY;ZZZZ", 1, "test.txt"),
                new RawFeedLine("ligne sans point virgule", 2, "test.txt"),
                new RawFeedLine("079;;23012025;DDDD;EEEE;FFFF", 3, "test.txt"), // msg-type vide
                new RawFeedLine("080;22;23012025;GGGG;HHHH;IIII", 4, "test.txt"),
                new RawFeedLine("un seul champ", 5, "test.txt")
        );

        ParsingResult result = feedParsingService.parseAndGroupLines(linesWithErrors);

        assertEquals(5, result.getTotalLinesProcessed());
        assertEquals(2, result.getValidLinesProcessed());
        assertEquals(3, result.getErrorLinesCount());
        assertEquals(40.0, result.getSuccessRate(), 0.01);
        assertEquals(2, result.getGroupCount());
        assertTrue(result.hasErrors());
        assertEquals(3, result.getParsingErrors().size());
    }

    @Test
    void testIsValidFeedLine() {
        assertTrue(feedParsingService.isValidFeedLine("077;20;23012025;XXXX;YYYY;ZZZZ"));
        assertTrue(feedParsingService.isValidFeedLine("field1;field2"));
        assertFalse(feedParsingService.isValidFeedLine("field1;"));
        assertFalse(feedParsingService.isValidFeedLine("field1"));
        assertFalse(feedParsingService.isValidFeedLine(""));
        assertFalse(feedParsingService.isValidFeedLine(null));
        assertFalse(feedParsingService.isValidFeedLine("   "));
    }

    @Test
    void testParsedFeedGroupProperties() {
        List<RawFeedLine> lines = List.of(
                new RawFeedLine("077;20;23012025;XXXX;YYYY;ZZZZ", 1, "test.txt"),
                new RawFeedLine("078;20;23012025;AAAA;BBBB;CCCC", 2, "test.txt")
        );

        ParsedFeedGroup group = new ParsedFeedGroup("20", lines);

        assertEquals("20", group.getMsgType());
        assertEquals(2, group.getTotalLines());
        assertEquals("test.txt", group.getSourceFileName());
        assertFalse(group.isEmpty());
        assertTrue(group.isValid());
        assertNotNull(group.getFirstLine());
        assertNotNull(group.getLastLine());
        assertEquals(1, group.getFirstLine().getLineNumber());
        assertEquals(2, group.getLastLine().getLineNumber());
    }
}
