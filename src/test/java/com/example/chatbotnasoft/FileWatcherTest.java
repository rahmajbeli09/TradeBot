package com.example.chatbotnasoft;

import com.example.chatbotnasoft.service.FileProcessingService;
import com.example.chatbotnasoft.service.FileStabilizationService;
import com.example.chatbotnasoft.service.FileWatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "file-watcher.input-directory=test-input/feeds",
    "file-watcher.stabilization-delay-minutes=1",
    "file-watcher.check-interval-seconds=5"
})
class FileWatcherTest {

    @Autowired
    private FileWatcherService fileWatcherService;

    @Autowired
    private FileStabilizationService stabilizationService;

    @Autowired
    private FileProcessingService processingService;

    private Path testInputDir;

    @BeforeEach
    void setUp() throws IOException {
        testInputDir = Paths.get("test-input/feeds");
        if (Files.exists(testInputDir)) {
            Files.walk(testInputDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignorer les erreurs de suppression
                        }
                    });
        }
        Files.createDirectories(testInputDir);
    }

    @Test
    void testFileWatcherInitialization() {
        assertTrue(fileWatcherService.isWatching());
        assertNotNull(stabilizationService);
        assertNotNull(processingService);
    }

    @Test
    void testFileDetectionAndStabilization() throws IOException, InterruptedException {
        // Créer un fichier FEED
        Path testFile = testInputDir.resolve("FEED20260205.txt");
        
        // Écrire du contenu initial
        Files.write(testFile, "Test content".getBytes());
        
        // Attendre un peu pour la détection
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Vérifier que le fichier est en cours de stabilisation
        assertTrue(stabilizationService.getStabilizingFiles().containsKey(testFile));
        
        // Ajouter du contenu pour simuler une modification
        Files.write(testFile, "Modified content".getBytes());
        
        // Attendre la stabilisation (1 minute configurée)
        try {
            TimeUnit.SECONDS.sleep(65);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Vérifier que le fichier est prêt
        assertTrue(processingService.isFileReady(testFile));
        assertNotNull(processingService.getReadyTime(testFile));
    }

    @Test
    void testFilePatternFiltering() throws IOException {
        // Créer un fichier qui ne correspond pas au pattern
        Path wrongFile = testInputDir.resolve("WRONGFILE.txt");
        Files.write(wrongFile, "Wrong content".getBytes());
        
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Le fichier ne doit pas être en stabilisation
        assertFalse(stabilizationService.getStabilizingFiles().containsKey(wrongFile));
        
        // Créer un fichier qui correspond au pattern
        Path correctFile = testInputDir.resolve("FEED20260205.txt");
        Files.write(correctFile, "Correct content".getBytes());
        
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Le fichier doit être en stabilisation
        assertTrue(stabilizationService.getStabilizingFiles().containsKey(correctFile));
    }
}
