package com.example.chatbotnasoft;

import com.example.chatbotnasoft.dto.RawFeedLine;
import com.example.chatbotnasoft.service.FileReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileReadingServiceTest {

    @Autowired
    private FileReadingService fileReadingService;

    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        testFile = Files.createTempFile("test-feed", ".txt");
        
        // Contenu de test avec différentes lignes
        String content = """
                Ligne 1 valide
                Ligne 2 avec des espaces   
                
                Ligne 4 après une ligne vide
                Ligne 5 avec    espaces    multiples
                  
                Ligne 7 finale
                """;
        
        Files.write(testFile, content.getBytes());
    }

    @Test
    void testReadFileLines() throws IOException {
        try (Stream<RawFeedLine> lines = fileReadingService.readFileLines(testFile)) {
            List<RawFeedLine> lineList = lines.toList();
            
            // Vérifier que les lignes vides sont ignorées
            assertEquals(5, lineList.size());
            
            // Vérifier le contenu
            assertEquals("Ligne 1 valide", lineList.get(0).getTrimmedContent());
            assertEquals("Ligne 2 avec des espaces", lineList.get(1).getTrimmedContent());
            assertEquals("Ligne 4 après une ligne vide", lineList.get(2).getTrimmedContent());
            assertEquals("Ligne 5 avec    espaces    multiples", lineList.get(3).getTrimmedContent());
            assertEquals("Ligne 7 finale", lineList.get(4).getTrimmedContent());
            
            // Vérifier les numéros de ligne
            assertEquals(1, lineList.get(0).getLineNumber());
            assertEquals(2, lineList.get(1).getLineNumber());
            assertEquals(4, lineList.get(2).getLineNumber());
            assertEquals(5, lineList.get(3).getLineNumber());
            assertEquals(7, lineList.get(4).getLineNumber());
            
            // Vérifier le nom du fichier source
            String fileName = testFile.getFileName().toString();
            lineList.forEach(line -> assertEquals(fileName, line.getSourceFileName()));
        }
    }

    @Test
    void testCountLines() throws IOException {
        long lineCount = fileReadingService.countLines(testFile);
        assertEquals(5, lineCount);
    }

    @Test
    void testIsValidFeedFile() {
        assertTrue(fileReadingService.isValidFeedFile(Path.of("FEED20260205.txt")));
        assertTrue(fileReadingService.isValidFeedFile(Path.of("FEED_TEST.txt")));
        assertFalse(fileReadingService.isValidFeedFile(Path.of("feed20260205.txt")));
        assertFalse(fileReadingService.isValidFeedFile(Path.of("FEED20260205.csv")));
        assertFalse(fileReadingService.isValidFeedFile(Path.of("OTHER.txt")));
    }

    @Test
    void testReadNonExistentFile() {
        Path nonExistent = Path.of("non-existent.txt");
        
        assertThrows(IOException.class, () -> {
            try (Stream<RawFeedLine> lines = fileReadingService.readFileLines(nonExistent)) {
                lines.toList();
            }
        });
    }

    @Test
    void testRawFeedLineProperties() {
        RawFeedLine line = new RawFeedLine("  test content  ", 42, "test.txt");
        
        assertEquals("  test content  ", line.getContent());
        assertEquals("test content", line.getTrimmedContent());
        assertEquals(42, line.getLineNumber());
        assertEquals("test.txt", line.getSourceFileName());
        assertTrue(line.isValid());
        assertFalse(line.isEmpty());
        assertNotNull(line.getReadAt());
        
        RawFeedLine emptyLine = new RawFeedLine("   ", 1, "test.txt");
        assertTrue(emptyLine.isEmpty());
        assertFalse(emptyLine.isValid());
    }
}
