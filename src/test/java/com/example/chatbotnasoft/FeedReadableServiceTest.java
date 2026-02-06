package com.example.chatbotnasoft;

import com.example.chatbotnasoft.dto.ReadableFeedLine;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import com.example.chatbotnasoft.service.FeedReadableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour FeedReadableService
 */
@ExtendWith(MockitoExtension.class)
class FeedReadableServiceTest {

    @Mock
    private FeedMappingRepository feedMappingRepository;

    @InjectMocks
    private FeedReadableService feedReadableService;

    private File tempFeedFile;
    private FeedMapping sampleMapping;

    @BeforeEach
    void setUp() throws IOException {
        // Créer un fichier FEED temporaire
        tempFeedFile = File.createTempFile("test_feed", ".txt");
        tempFeedFile.deleteOnExit();

        // Créer un mapping de test
        Map<String, String> mappingFields = new LinkedHashMap<>();
        mappingFields.put("Champ 1", "Type d'enregistrement");
        mappingFields.put("Champ 2", "Code de statut");
        mappingFields.put("Champ 3", "Identifiant principal");
        mappingFields.put("Champ 4", "Identifiant secondaire");
        mappingFields.put("Champ 5", "Référence opération");
        mappingFields.put("Champ 6", "Valeur numérique 1");

        sampleMapping = new FeedMapping();
        sampleMapping.setMsgType("05");
        sampleMapping.setMapping(mappingFields);
    }

    @Test
    void testGenerateReadableFeed_WithValidMapping() throws IOException {
        // Préparer le fichier de test
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping));

        // Exécuter le test
        List<ReadableFeedLine> result = feedReadableService.generateReadableFeed(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(1, result.size());
        ReadableFeedLine line = result.get(0);
        assertEquals("05", line.getMsgType());
        assertTrue(line.hasMapping());
        assertNull(line.getErreur());

        // Vérifier les champs lisibles
        Map<String, String> champsLisibles = line.getChampsLisibles();
        assertEquals(6, champsLisibles.size());
        assertEquals("Type d'enregistrement : 061", champsLisibles.get("Champ 1"));
        assertEquals("Code de statut : 05", champsLisibles.get("Champ 2"));
        assertEquals("Identifiant principal : 20250613", champsLisibles.get("Champ 3"));
        assertEquals("Identifiant secondaire : 062116", champsLisibles.get("Champ 4"));
        assertEquals("Référence opération : TN823JXM7T75", champsLisibles.get("Champ 5"));
        assertEquals("Valeur numérique 1 : A", champsLisibles.get("Champ 6"));

        verify(feedMappingRepository, times(1)).findByMsgType("05");
    }

    @Test
    void testGenerateReadableFeed_WithoutMapping() throws IOException {
        // Préparer le fichier de test
        String testContent = "145;A3;20250613;062116;TN823JXM7T75;C";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository (pas de mapping trouvé)
        when(feedMappingRepository.findByMsgType("A3")).thenReturn(Optional.empty());

        // Exécuter le test
        List<ReadableFeedLine> result = feedReadableService.generateReadableFeed(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(1, result.size());
        ReadableFeedLine line = result.get(0);
        assertEquals("A3", line.getMsgType());
        assertFalse(line.hasMapping());
        assertNotNull(line.getErreur());
        assertTrue(line.getErreur().contains("Aucun mapping trouvé"));

        verify(feedMappingRepository, times(1)).findByMsgType("A3");
    }

    @Test
    void testGenerateReadableFeed_WithIncompleteMapping() throws IOException {
        // Préparer le fichier de test avec plus de champs que le mapping
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A;EXTRA;CHAMP;SUPPLEMENTAIRE";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping));

        // Exécuter le test
        List<ReadableFeedLine> result = feedReadableService.generateReadableFeed(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(1, result.size());
        ReadableFeedLine line = result.get(0);
        assertTrue(line.hasMapping());

        // Vérifier les champs lisibles (certains avec mapping, d'autres "Inconnu")
        Map<String, String> champsLisibles = line.getChampsLisibles();
        assertEquals(9, champsLisibles.size());
        
        // Champs avec mapping
        assertEquals("Type d'enregistrement : 061", champsLisibles.get("Champ 1"));
        assertEquals("Code de statut : 05", champsLisibles.get("Champ 2"));
        
        // Champs sans mapping
        assertEquals("Inconnu : EXTRA", champsLisibles.get("Champ 7"));
        assertEquals("Inconnu : CHAMP", champsLisibles.get("Champ 8"));
        assertEquals("Inconnu : SUPPLEMENTAIRE", champsLisibles.get("Champ 9"));
    }

    @Test
    void testGenerateReadableFeed_WithInvalidLine() throws IOException {
        // Préparer le fichier de test avec une ligne invalide (moins de 2 champs)
        String testContent = "061"; // Seulement 1 champ
        writeToFile(tempFeedFile, testContent);

        // Exécuter le test
        List<ReadableFeedLine> result = feedReadableService.generateReadableFeed(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(1, result.size());
        ReadableFeedLine line = result.get(0);
        assertFalse(line.hasMapping());
        assertNotNull(line.getErreur());
        assertTrue(line.getErreur().contains("Moins de 2 champs trouvés"));

        // Ne devrait pas appeler le repository
        verify(feedMappingRepository, never()).findByMsgType(anyString());
    }

    @Test
    void testGenerateReadableFeed_WithMultipleLines() throws IOException {
        // Préparer le fichier de test avec plusieurs lignes
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A\n" +
                           "145;A3;20250613;062116;TN823JXM7T75;C\n" +
                           "023;16;20250613;000000;32;C";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping));
        when(feedMappingRepository.findByMsgType("A3")).thenReturn(Optional.empty());
        when(feedMappingRepository.findByMsgType("16")).thenReturn(Optional.empty());

        // Exécuter le test
        List<ReadableFeedLine> result = feedReadableService.generateReadableFeed(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(3, result.size());

        // Première ligne (avec mapping)
        ReadableFeedLine line1 = result.get(0);
        assertEquals("05", line1.getMsgType());
        assertTrue(line1.hasMapping());

        // Deuxième ligne (sans mapping)
        ReadableFeedLine line2 = result.get(1);
        assertEquals("A3", line2.getMsgType());
        assertFalse(line2.hasMapping());

        // Troisième ligne (sans mapping)
        ReadableFeedLine line3 = result.get(2);
        assertEquals("16", line3.getMsgType());
        assertFalse(line3.hasMapping());

        verify(feedMappingRepository, times(1)).findByMsgType("05");
        verify(feedMappingRepository, times(1)).findByMsgType("A3");
        verify(feedMappingRepository, times(1)).findByMsgType("16");
    }

    @Test
    void testGenerateTextSummary() throws IOException {
        // Préparer le fichier de test
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping));

        // Exécuter le test
        List<ReadableFeedLine> readableLines = feedReadableService.generateReadableFeed(tempFeedFile.getAbsolutePath());
        String summary = feedReadableService.generateTextSummary(readableLines);

        // Vérifications
        assertNotNull(summary);
        assertTrue(summary.contains("RÉSUMÉ LISIBLE DU FICHIER FEED"));
        assertTrue(summary.contains("STATISTIQUES"));
        assertTrue(summary.contains("MSG-TYPES TRAITÉS"));
        assertTrue(summary.contains("Lignes totales: 1"));
        assertTrue(summary.contains("Lignes avec mapping: 1"));
        assertTrue(summary.contains("05: 1 ligne(s)"));
        assertTrue(summary.contains("Type d'enregistrement : 061"));
    }

    @Test
    void testListAvailableFeedFiles() {
        // Ce test dépend de l'existence du répertoire input/feeds
        // Dans un environnement réel, il faudrait créer des fichiers de test
        List<String> files = feedReadableService.listAvailableFeedFiles();
        
        // Vérifier que la méthode ne lève pas d'exception
        assertNotNull(files);
        assertTrue(files instanceof List<?>);
    }

    /**
     * Méthode utilitaire pour écrire du contenu dans un fichier
     */
    private void writeToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
