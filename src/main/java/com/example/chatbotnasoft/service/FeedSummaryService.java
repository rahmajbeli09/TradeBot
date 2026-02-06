package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.ReadableFeedLine;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Service pour générer des résumés lisibles des fichiers FEED
 * en utilisant les mappings existants dans MongoDB
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedSummaryService {
    
    private final FeedMappingRepository feedMappingRepository;
    
    /**
     * Génère un résumé lisible pour chaque ligne d'un fichier FEED
     * @param filePath Chemin du fichier FEED à analyser
     * @return Liste des lignes lisibles avec leurs significations
     */
    public List<ReadableFeedLine> generateReadableSummary(String filePath) {
        List<ReadableFeedLine> readableLines = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Ignorer les lignes vides
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Traiter chaque ligne individuellement
                ReadableFeedLine readableLine = processLine(line, lineNumber);
                if (readableLine != null) {
                    readableLines.add(readableLine);
                }
            }
            
            log.info("✅ Traitement terminé : {} lignes lisibles générées depuis {}", readableLines.size(), filePath);
            
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Erreur de lecture du fichier", e);
        }
        
        return readableLines;
    }
    
    /**
     * Traite une ligne individuelle et la rend lisible
     * @param line Ligne à traiter
     * @param lineNumber Numéro de la ligne (pour logging)
     * @return ReadableFeedLine ou null si impossible de traiter
     */
    private ReadableFeedLine processLine(String line, int lineNumber) {
        try {
            // Extraire les champs séparés par ";"
            String[] champs = line.split(";");
            
            if (champs.length < 2) {
                log.warn("⚠️ Ligne {} ignorée : moins de 2 champs", lineNumber);
                return null;
            }
            
            // Extraire le msgType (deuxième champ)
            String msgType = champs[1].trim();
            
            // Rechercher le mapping dans MongoDB
            Optional<FeedMapping> feedMappingOpt = feedMappingRepository.findByMsgType(msgType);
            
            if (feedMappingOpt.isEmpty()) {
                log.warn("⚠️ Ligne {} : Aucun mapping trouvé pour msgType '{}'", lineNumber, msgType);
                return null;
            }
            
            // Créer la représentation lisible
            Map<String, String> champsLisibles = createReadableMapping(champs, feedMappingOpt.get());
            
            return new ReadableFeedLine(msgType, champsLisibles);
            
        } catch (Exception e) {
            log.error("❌ Erreur traitement ligne {} : {}", lineNumber, e.getMessage());
            return null;
        }
    }
    
    /**
     * Crée le mapping lisible entre les champs et leurs significations
     * @param champs Tableau des valeurs de la ligne
     * @param feedMapping Mapping trouvé en base
     * @return Map avec "Champ X" -> "Signification : Valeur"
     */
    private Map<String, String> createReadableMapping(String[] champs, FeedMapping feedMapping) {
        Map<String, String> readableMapping = new LinkedHashMap<>();
        
        // Pour chaque champ de la ligne
        for (int i = 0; i < champs.length; i++) {
            String champNom = "Champ " + (i + 1);
            String valeur = champs[i].trim();
            
            // Chercher la signification dans le mapping (index i -> signification)
            String signification = getSignificationForIndex(feedMapping, i);
            
            // Construire la représentation lisible
            String representation;
            if (signification != null && !signification.trim().isEmpty()) {
                representation = signification + " : " + valeur;
            } else {
                representation = "Non défini : " + valeur;
            }
            
            readableMapping.put(champNom, representation);
        }
        
        return readableMapping;
    }
    
    /**
     * Récupère la signification pour un index de champ donné
     * @param feedMapping Mapping du feed
     * @param index Index du champ (0-based)
     * @return Signification ou null si non trouvée
     */
    private String getSignificationForIndex(FeedMapping feedMapping, int index) {
        // Le mapping est stocké comme "Champ X" -> signification
        String champKey = "Champ " + (index + 1);
        return feedMapping.getMapping().get(champKey);
    }
    
    /**
     * Génère un résumé textuel global des lignes lisibles
     * @param readableLines Liste des lignes traitées
     * @return Résumé textuel formaté
     */
    public String generateTextSummary(List<ReadableFeedLine> readableLines) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("=== RÉSUMÉ LISIBLE DU FICHIER FEED ===\n\n");
        
        for (int i = 0; i < readableLines.size(); i++) {
            ReadableFeedLine line = readableLines.get(i);
            
            summary.append("Ligne ").append(i + 1).append(" (msgType: ").append(line.getMsgType()).append("):\n");
            
            for (Map.Entry<String, String> entry : line.getChampsLisibles().entrySet()) {
                summary.append("  ").append(entry.getKey()).append(" → ").append(entry.getValue()).append("\n");
            }
            
            summary.append("\n");
        }
        
        summary.append("=== TOTAL : ").append(readableLines.size()).append(" lignes traitées ===");
        
        return summary.toString();
    }
}
