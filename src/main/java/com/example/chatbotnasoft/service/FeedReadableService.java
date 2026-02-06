package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.ReadableFeedLine;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class FeedReadableService {

    @Autowired
    private FeedMappingRepository feedMappingRepository;

    /**
     * Génère une représentation lisible d'un fichier FEED
     * @param filePath Chemin du fichier FEED à analyser
     * @return Liste des lignes lisibles avec leurs significations
     */
    public List<ReadableFeedLine> generateReadableFeed(String filePath) {
        log.info("📖 Début de la lecture lisible du fichier: {}", filePath);
        
        List<ReadableFeedLine> readableLines = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String ligne;
            int numeroLigne = 0;
            
            while ((ligne = reader.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.trim();
                
                // Ignorer les lignes vides
                if (ligne.isEmpty()) {
                    continue;
                }
                
                log.debug("📝 Lecture ligne {}: {}", numeroLigne, ligne);
                
                ReadableFeedLine readableLine = processLine(ligne, numeroLigne);
                readableLines.add(readableLine);
            }
            
            log.info("✅ Lecture terminée: {} lignes traitées", readableLines.size());
            
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Erreur de lecture du fichier: " + e.getMessage(), e);
        }
        
        return readableLines;
    }

    /**
     * Traite une ligne individuelle pour la rendre lisible
     * @param ligne Ligne FEED à traiter
     * @param numeroLigne Numéro de la ligne pour le logging
     * @return ReadableFeedLine avec les champs lisibles
     */
    private ReadableFeedLine processLine(String ligne, int numeroLigne) {
        try {
            // Extraire les champs séparés par ";"
            String[] champs = ligne.split(";");
            
            // Vérifier qu'on a au moins 2 champs pour extraire le msgType
            if (champs.length < 2) {
                String erreur = "Ligne " + numeroLigne + ": Moins de 2 champs trouvés";
                log.warn("⚠️ {}", erreur);
                return new ReadableFeedLine("", ligne, erreur);
            }
            
            // Extraire le msgType (2ème champ)
            String msgType = champs[1].trim();
            log.debug("🔍 MsgType extrait: '{}' pour ligne {}", msgType, numeroLigne);
            
            // Chercher le mapping dans MongoDB
            Optional<FeedMapping> mappingOpt = feedMappingRepository.findByMsgType(msgType);
            
            if (mappingOpt.isEmpty()) {
                String erreur = "Aucun mapping trouvé pour le msgType: " + msgType;
                log.warn("⚠️ Ligne {}: {}", numeroLigne, erreur);
                return new ReadableFeedLine(msgType, ligne, erreur);
            }
            
            // Générer les champs lisibles
            Map<String, String> champsLisibles = generateReadableFields(champs, mappingOpt.get());
            
            log.debug("✅ Ligne {} traitée avec succès - {} champs lisibles", numeroLigne, champsLisibles.size());
            return new ReadableFeedLine(msgType, champsLisibles, ligne);
            
        } catch (Exception e) {
            String erreur = "Erreur lors du traitement de la ligne " + numeroLigne + ": " + e.getMessage();
            log.error("❌ {}", erreur, e);
            return new ReadableFeedLine("", ligne, erreur);
        }
    }

    /**
     * Génère les champs lisibles en associant chaque valeur à sa signification
     * @param champs Valeurs des champs de la ligne
     * @param mapping Mapping trouvé dans MongoDB
     * @return Map des champs lisibles (Champ X -> Signification : Valeur)
     */
    private Map<String, String> generateReadableFields(String[] champs, FeedMapping mapping) {
        Map<String, String> champsLisibles = new LinkedHashMap<>();
        Map<String, String> mappingFields = mapping.getMapping();
        
        // Pour chaque champ de la ligne, essayer de trouver sa signification
        for (int i = 0; i < champs.length; i++) {
            String champKey = "Champ " + (i + 1);
            String champValue = champs[i].trim();
            
            // Chercher la signification dans le mapping
            String signification = mappingFields.get(champKey);
            
            if (signification != null) {
                // Mapping trouvé : "Signification : Valeur"
                String readableValue = signification + " : " + champValue;
                champsLisibles.put(champKey, readableValue);
            } else {
                // Pas de mapping trouvé : "Inconnu : Valeur"
                String readableValue = "Inconnu : " + champValue;
                champsLisibles.put(champKey, readableValue);
            }
        }
        
        return champsLisibles;
    }

    /**
     * Liste les fichiers FEED disponibles dans le répertoire input/feeds
     * @return Liste des noms de fichiers FEED
     */
    public List<String> listAvailableFeedFiles() {
        List<String> feedFiles = new ArrayList<>();
        java.io.File feedDir = new java.io.File("input/feeds");
        
        if (feedDir.exists() && feedDir.isDirectory()) {
            java.io.File[] files = feedDir.listFiles((dir, name) -> name.startsWith("FEED") && name.endsWith(".txt"));
            
            if (files != null) {
                for (java.io.File file : files) {
                    feedFiles.add(file.getName());
                }
            }
        }
        
        Collections.sort(feedFiles);
        log.info("📂 {} fichiers FEED trouvés: {}", feedFiles.size(), feedFiles);
        return feedFiles;
    }

    /**
     * Génère un résumé textuel des lignes lisibles
     * @param readableLines Liste des lignes lisibles
     * @return Résumé textuel formaté
     */
    public String generateTextSummary(List<ReadableFeedLine> readableLines) {
        StringBuilder summary = new StringBuilder();
        summary.append("📊 RÉSUMÉ LISIBLE DU FICHIER FEED\n");
        summary.append("=====================================\n\n");
        
        int lignesTraitees = 0;
        int lignesAvecMapping = 0;
        int lignesSansMapping = 0;
        Map<String, Integer> msgTypeCount = new HashMap<>();
        
        for (ReadableFeedLine line : readableLines) {
            lignesTraitees++;
            
            if (line.hasMapping()) {
                lignesAvecMapping++;
                
                // Compter les msgTypes
                String msgType = line.getMsgType();
                msgTypeCount.put(msgType, msgTypeCount.getOrDefault(msgType, 0) + 1);
                
                summary.append("🔹 Ligne ").append(lignesTraitees).append(" (MsgType: ").append(line.getMsgType()).append(")\n");
                
                for (Map.Entry<String, String> entry : line.getChampsLisibles().entrySet()) {
                    summary.append("   ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                summary.append("\n");
                
            } else {
                lignesSansMapping++;
                summary.append("❌ Ligne ").append(lignesTraitees).append(": ").append(line.getErreur()).append("\n");
                summary.append("   Originale: ").append(line.getLigneOriginale()).append("\n\n");
            }
        }
        
        // Résumé statistique
        summary.append("📈 STATISTIQUES\n");
        summary.append("==============\n");
        summary.append("• Lignes totales: ").append(lignesTraitees).append("\n");
        summary.append("• Lignes avec mapping: ").append(lignesAvecMapping).append("\n");
        summary.append("• Lignes sans mapping: ").append(lignesSansMapping).append("\n");
        summary.append("• Taux de succès: ").append(String.format("%.1f", (lignesAvecMapping * 100.0 / lignesTraitees))).append("%\n\n");
        
        summary.append("🏷️ MSG-TYPES TRAITÉS\n");
        summary.append("=====================\n");
        for (Map.Entry<String, Integer> entry : msgTypeCount.entrySet()) {
            summary.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" ligne(s)\n");
        }
        
        return summary.toString();
    }
}
