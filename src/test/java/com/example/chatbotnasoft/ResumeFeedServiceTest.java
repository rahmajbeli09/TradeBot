package com.example.chatbotnasoft;

import com.example.chatbotnasoft.dto.ResumeData;
import com.example.chatbotnasoft.dto.ResumeResponse;
import com.example.chatbotnasoft.dto.ResumeStatistiques;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import com.example.chatbotnasoft.service.ResumeFeedService;
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
 * Tests unitaires pour ResumeFeedService
 */
@ExtendWith(MockitoExtension.class)
class ResumeFeedServiceTest {

    @Mock
    private FeedMappingRepository feedMappingRepository;

    @InjectMocks
    private ResumeFeedService resumeFeedService;

    private File tempFeedFile;
    private FeedMapping sampleMapping05;
    private FeedMapping sampleMappingA3;

    @BeforeEach
    void setUp() throws IOException {
        // Créer un fichier FEED temporaire
        tempFeedFile = File.createTempFile("test_resume_feed", ".txt");
        tempFeedFile.deleteOnExit();

        // Créer des mappings de test
        Map<String, String> mapping05 = new LinkedHashMap<>();
        mapping05.put("Champ 1", "Type d'enregistrement");
        mapping05.put("Champ 2", "Code de statut");
        mapping05.put("Champ 3", "Identifiant principal");
        mapping05.put("Champ 4", "Identifiant secondaire");
        mapping05.put("Champ 5", "Référence opération");
        mapping05.put("Champ 6", "Valeur numérique 1");

        sampleMapping05 = new FeedMapping();
        sampleMapping05.setMsgType("05");
        sampleMapping05.setMapping(mapping05);

        Map<String, String> mappingA3 = new LinkedHashMap<>();
        mappingA3.put("Champ 1", "Type de transaction");
        mappingA3.put("Champ 2", "Code de sous-transaction");
        mappingA3.put("Champ 3", "Date de transaction");
        mappingA3.put("Champ 4", "Heure de transaction");
        mappingA3.put("Champ 5", "Référence unique");
        mappingA3.put("Champ 6", "Montant");

        sampleMappingA3 = new FeedMapping();
        sampleMappingA3.setMsgType("A3");
        sampleMappingA3.setMapping(mappingA3);
    }

    @Test
    void testGenerateResume_WithValidMappings() throws IOException {
        // Préparer le fichier de test
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A\n" +
                           "145;A3;20250613;062116;TN823JXM7T75;C";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping05));
        when(feedMappingRepository.findByMsgType("A3")).thenReturn(Optional.of(sampleMappingA3));

        // Exécuter le test
        ResumeResponse result = resumeFeedService.generateResume(tempFeedFile.getAbsolutePath());

        // Vérifications générales
        assertNotNull(result);
        assertNotNull(result.getResumeData());
        assertNotNull(result.getResumeTexte());
        assertNotNull(result.getStatistiques());
        assertEquals(2, result.getResumeData().size());

        // Vérifier la première ligne (msgType 05)
        ResumeData line1 = result.getResumeData().get(0);
        assertEquals("05", line1.getMsgType());
        assertTrue(line1.isMappingTrouve());
        assertNotNull(line1.getMapping());
        assertNotNull(line1.getValeurs());
        assertEquals(6, line1.getValeurs().size());
        assertEquals("061", line1.getValeurs().get("Champ 1"));
        assertEquals("05", line1.getValeurs().get("Champ 2"));
        assertEquals("Type d'enregistrement", line1.getMapping().get("Champ 1"));

        // Vérifier la deuxième ligne (msgType A3)
        ResumeData line2 = result.getResumeData().get(1);
        assertEquals("A3", line2.getMsgType());
        assertTrue(line2.isMappingTrouve());
        assertEquals(6, line2.getValeurs().size());
        assertEquals("145", line2.getValeurs().get("Champ 1"));
        assertEquals("A3", line2.getValeurs().get("Champ 2"));
        assertEquals("Type de transaction", line2.getMapping().get("Champ 1"));

        // Vérifier les statistiques
        ResumeStatistiques stats = result.getStatistiques();
        assertEquals(2, stats.getTotalLignes());
        assertEquals(2, stats.getLignesAvecMapping());
        assertEquals(0, stats.getLignesSansMapping());
        assertEquals(100.0, stats.getTauxSucces(), 0.01);
        assertEquals(2, stats.getMsgTypesUniques().size());
        assertTrue(stats.getMsgTypesUniques().contains("05"));
        assertTrue(stats.getMsgTypesUniques().contains("A3"));

        // Vérifier le résumé textuel
        String resumeTexte = result.getResumeTexte();
        assertNotNull(resumeTexte);
        assertTrue(resumeTexte.contains("RÉSUMÉ GLOBAL DU FICHIER FEED"));
        assertTrue(resumeTexte.contains("STATISTIQUES GÉNÉRALES"));
        assertTrue(resumeTexte.contains("RÉPARTITION PAR MSG-TYPE"));
        assertTrue(resumeTexte.contains("DÉTAILS PAR MSG-TYPE"));
        assertTrue(resumeTexte.contains("05: 1 ligne(s)"));
        assertTrue(resumeTexte.contains("A3: 1 ligne(s)"));

        verify(feedMappingRepository, times(1)).findByMsgType("05");
        verify(feedMappingRepository, times(1)).findByMsgType("A3");
    }

    @Test
    void testGenerateResume_WithMissingMapping() throws IOException {
        // Préparer le fichier de test
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A\n" +
                           "145;XX;20250613;062116;TN823JXM7T75;C";
        writeToFile(tempFeedFile, testContent);

        // Mock du repository (XX n'a pas de mapping)
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping05));
        when(feedMappingRepository.findByMsgType("XX")).thenReturn(Optional.empty());

        // Exécuter le test
        ResumeResponse result = resumeFeedService.generateResume(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(2, result.getResumeData().size());

        // Première ligne (avec mapping)
        ResumeData line1 = result.getResumeData().get(0);
        assertEquals("05", line1.getMsgType());
        assertTrue(line1.isMappingTrouve());

        // Deuxième ligne (sans mapping)
        ResumeData line2 = result.getResumeData().get(1);
        assertEquals("XX", line2.getMsgType());
        assertFalse(line2.isMappingTrouve());
        assertNotNull(line2.getErreur());
        assertTrue(line2.getErreur().contains("Aucun mapping trouvé"));

        // Vérifier les statistiques
        ResumeStatistiques stats = result.getStatistiques();
        assertEquals(2, stats.getTotalLignes());
        assertEquals(1, stats.getLignesAvecMapping());
        assertEquals(1, stats.getLignesSansMapping());
        assertEquals(50.0, stats.getTauxSucces(), 0.01);

        verify(feedMappingRepository, times(1)).findByMsgType("05");
        verify(feedMappingRepository, times(1)).findByMsgType("XX");
    }

    @Test
    void testGenerateResume_WithInvalidLine() throws IOException {
        // Préparer le fichier de test avec une ligne invalide
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A\n" +
                           "INVALID_LINE"; // Seulement 1 champ
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping05));

        // Exécuter le test
        ResumeResponse result = resumeFeedService.generateResume(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(2, result.getResumeData().size());

        // Première ligne (valide)
        ResumeData line1 = result.getResumeData().get(0);
        assertEquals("05", line1.getMsgType());
        assertTrue(line1.isMappingTrouve());

        // Deuxième ligne (invalide)
        ResumeData line2 = result.getResumeData().get(1);
        assertEquals("", line2.getMsgType());
        assertFalse(line2.isMappingTrouve());
        assertNotNull(line2.getErreur());
        assertTrue(line2.getErreur().contains("Moins de 2 champs trouvés"));

        verify(feedMappingRepository, times(1)).findByMsgType("05");
        verify(feedMappingRepository, never()).findByMsgType(eq(""));
    }

    @Test
    void testGenerateResume_WithDifferentFieldCounts() throws IOException {
        // Préparer le fichier de test avec différents nombres de champs
        String testContent = "061;05;20250613;062116;TN823JXM7T75;A\n" +  // 6 champs
                           "145;A3;20250613;062116;TN823JXM7T75;C;EXTRA;FIELD"; // 8 champs
        writeToFile(tempFeedFile, testContent);

        // Mock du repository
        when(feedMappingRepository.findByMsgType("05")).thenReturn(Optional.of(sampleMapping05));
        when(feedMappingRepository.findByMsgType("A3")).thenReturn(Optional.of(sampleMappingA3));

        // Exécuter le test
        ResumeResponse result = resumeFeedService.generateResume(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertEquals(2, result.getResumeData().size());

        // Première ligne (6 champs)
        ResumeData line1 = result.getResumeData().get(0);
        assertEquals("05", line1.getMsgType());
        assertTrue(line1.isMappingTrouve());
        assertEquals(6, line1.getValeurs().size());

        // Deuxième ligne (8 champs)
        ResumeData line2 = result.getResumeData().get(1);
        assertEquals("A3", line2.getMsgType());
        assertTrue(line2.isMappingTrouve());
        assertEquals(8, line2.getValeurs().size());
        assertEquals("EXTRA", line2.getValeurs().get("Champ 7"));
        assertEquals("FIELD", line2.getValeurs().get("Champ 8"));

        verify(feedMappingRepository, times(1)).findByMsgType("05");
        verify(feedMappingRepository, times(1)).findByMsgType("A3");
    }

    @Test
    void testGenerateResume_EmptyFile() throws IOException {
        // Créer un fichier vide
        writeToFile(tempFeedFile, "");

        // Exécuter le test
        ResumeResponse result = resumeFeedService.generateResume(tempFeedFile.getAbsolutePath());

        // Vérifications
        assertNotNull(result);
        assertEquals(0, result.getResumeData().size());
        assertNotNull(result.getResumeTexte());
        assertNotNull(result.getStatistiques());

        ResumeStatistiques stats = result.getStatistiques();
        assertEquals(0, stats.getTotalLignes());
        assertEquals(0, stats.getLignesAvecMapping());
        assertEquals(0, stats.getLignesSansMapping());
        assertEquals(0.0, stats.getTauxSucces(), 0.01);
        assertTrue(stats.getMsgTypesUniques().isEmpty());

        // Ne devrait appeler aucun repository
        verify(feedMappingRepository, never()).findByMsgType(anyString());
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
