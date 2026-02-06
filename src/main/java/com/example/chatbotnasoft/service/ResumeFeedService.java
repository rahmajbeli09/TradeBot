package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.ResumeData;
import com.example.chatbotnasoft.dto.ResumeResponse;
import com.example.chatbotnasoft.dto.ResumeStatistiques;
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
 * Service pour générer des résumés structurés des fichiers FEED
 */
@Service
@Slf4j
public class ResumeFeedService {

    @Autowired
    private FeedMappingRepository feedMappingRepository;

    /**
     * Génère un résumé complet structuré d'un fichier FEED
     * @param filePath Chemin du fichier FEED à analyser
     * @return ResumeResponse avec les données structurées et le résumé textuel
     */
    public ResumeResponse generateResume(String filePath) {
        log.info("📊 Début de la génération de résumé pour: {}", filePath);
        
        List<ResumeData> resumeDataList = new ArrayList<>();
        
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
                
                log.debug("📝 Traitement ligne {}: {}", numeroLigne, ligne);
                
                ResumeData resumeData = processLineToResumeData(ligne, numeroLigne);
                resumeDataList.add(resumeData);
            }
            
            log.info("✅ Traitement terminé: {} lignes analysées", resumeDataList.size());
            
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Erreur de lecture du fichier: " + e.getMessage(), e);
        }
        
        // Générer le résumé textuel global
        String resumeTexte = generateResumeTexte(resumeDataList);
        
        return new ResumeResponse(resumeDataList, resumeTexte);
    }

    /**
     * Traite une ligne individuelle pour créer un ResumeData
     * @param ligne Ligne FEED à traiter
     * @param numeroLigne Numéro de la ligne pour le logging
     * @return ResumeData avec les données structurées
     */
    private ResumeData processLineToResumeData(String ligne, int numeroLigne) {
        try {
            // Extraire les champs séparés par ";"
            String[] champs = ligne.split(";");
            
            // Vérifier qu'on a au moins 2 champs pour extraire le msgType
            if (champs.length < 2) {
                String erreur = "Ligne " + numeroLigne + ": Moins de 2 champs trouvés";
                log.warn("⚠️ {}", erreur);
                return new ResumeData("", ligne, erreur);
            }
            
            // Extraire le msgType (2ème champ)
            String msgType = champs[1].trim();
            log.debug("🔍 MsgType extrait: '{}' pour ligne {}", msgType, numeroLigne);
            
            // Chercher le mapping dans MongoDB
            Optional<FeedMapping> mappingOpt = feedMappingRepository.findByMsgType(msgType);
            
            if (mappingOpt.isEmpty()) {
                String erreur = "Aucun mapping trouvé pour le msgType: " + msgType;
                log.warn("⚠️ Ligne {}: {}", numeroLigne, erreur);
                return new ResumeData(msgType, ligne, erreur);
            }
            
            // Créer les maps de valeurs et de mapping
            Map<String, String> valeursMap = createValeursMap(champs);
            Map<String, String> mappingMap = mappingOpt.get().getMapping();
            
            log.debug("✅ Ligne {} traitée avec succès - {} champs", numeroLigne, valeursMap.size());
            return new ResumeData(msgType, mappingMap, valeursMap, ligne);
            
        } catch (Exception e) {
            String erreur = "Erreur lors du traitement de la ligne " + numeroLigne + ": " + e.getMessage();
            log.error("❌ {}", erreur, e);
            return new ResumeData("", ligne, erreur);
        }
    }

    /**
     * Crée une map des valeurs à partir des champs de la ligne
     * @param champs Tableau des champs de la ligne
     * @return Map avec "Champ i" comme clé et la valeur brute comme valeur
     */
    private Map<String, String> createValeursMap(String[] champs) {
        Map<String, String> valeursMap = new LinkedHashMap<>();
        
        for (int i = 0; i < champs.length; i++) {
            String champKey = "Champ " + (i + 1);
            String champValue = champs[i].trim();
            valeursMap.put(champKey, champValue);
        }
        
        return valeursMap;
    }

    /**
     * Génère un résumé textuel global lisible pour l'agent
     * @param resumeDataList Liste des données résumées
     * @return Résumé textuel formaté
     */
    private String generateResumeTexte(List<ResumeData> resumeDataList) {
        StringBuilder resume = new StringBuilder();
        
        // En-tête du résumé
        resume.append("📊 RÉSUMÉ GLOBAL DU FICHIER FEED\n");
        resume.append("=====================================\n\n");
        
        // Statistiques générales
        ResumeStatistiques stats = new ResumeStatistiques(resumeDataList);
        resume.append("📈 STATISTIQUES GÉNÉRALES\n");
        resume.append("=============================\n");
        resume.append("• Lignes totales traitées: ").append(stats.getTotalLignes()).append("\n");
        resume.append("• Lignes avec mapping: ").append(stats.getLignesAvecMapping()).append("\n");
        resume.append("• Lignes sans mapping: ").append(stats.getLignesSansMapping()).append("\n");
        resume.append("• Taux de succès: ").append(String.format("%.1f", stats.getTauxSucces())).append("%\n");
        resume.append("• Types de messages différents: ").append(stats.getMsgTypesUniques().size()).append("\n\n");
        
        // Répartition par msgType
        resume.append("🏷️ RÉPARTITION PAR MSG-TYPE\n");
        resume.append("============================\n");
        Map<String, Integer> msgTypeCount = stats.getMsgTypeCount();
        List<String> sortedMsgTypes = new ArrayList<>(msgTypeCount.keySet());
        Collections.sort(sortedMsgTypes);
        
        for (String msgType : sortedMsgTypes) {
            int count = msgTypeCount.get(msgType);
            resume.append("• ").append(msgType).append(": ").append(count).append(" ligne(s)\n");
        }
        resume.append("\n");
        
        // Détails par msgType
        resume.append("🔍 DÉTAILS PAR MSG-TYPE\n");
        resume.append("=========================\n");
        
        // Grouper les lignes par msgType
        Map<String, List<ResumeData>> groupedByMsgType = new HashMap<>();
        for (ResumeData data : resumeDataList) {
            if (data.isMappingTrouve()) {
                String msgType = data.getMsgType();
                groupedByMsgType.computeIfAbsent(msgType, k -> new ArrayList<>()).add(data);
            }
        }
        
        // Générer les détails pour chaque msgType
        for (String msgType : sortedMsgTypes) {
            List<ResumeData> msgTypeLines = groupedByMsgType.get(msgType);
            if (msgTypeLines != null && !msgTypeLines.isEmpty()) {
                resume.append("📋 MsgType: ").append(msgType).append(" (").append(msgTypeLines.size()).append(" lignes)\n");
                
                // Prendre les 3 premières lignes comme aperçu
                int previewCount = Math.min(3, msgTypeLines.size());
                for (int i = 0; i < previewCount; i++) {
                    ResumeData data = msgTypeLines.get(i);
                    resume.append("   ").append(i + 1).append(". ");
                    
                    // Afficher les champs principaux (5 premiers)
                    Map<String, String> valeurs = data.getValeurs();
                    Map<String, String> mapping = data.getMapping();
                    
                    int fieldCount = Math.min(5, valeurs.size());
                    for (int j = 0; j < fieldCount; j++) {
                        String champKey = "Champ " + (j + 1);
                        String valeur = valeurs.get(champKey);
                        String signification = mapping != null ? mapping.get(champKey) : "Inconnu";
                        
                        resume.append(signification).append(": ").append(valeur);
                        if (j < fieldCount - 1) resume.append(" | ");
                    }
                    
                    if (valeurs.size() > 5) {
                        resume.append(" ... (+").append(valeurs.size() - 5).append(" champs)");
                    }
                    resume.append("\n");
                }
                
                if (msgTypeLines.size() > 3) {
                    resume.append("   ... et ").append(msgTypeLines.size() - 3).append(" autres lignes similaires\n");
                }
                resume.append("\n");
            }
        }
        
        // Lignes avec erreurs
        resume.append("❌ LIGNES AVEC ERREURS\n");
        resume.append("========================\n");
        int errorCount = 0;
        for (ResumeData data : resumeDataList) {
            if (!data.isMappingTrouve()) {
                errorCount++;
                resume.append(errorCount).append(". ").append(data.getErreur()).append("\n");
                resume.append("   Ligne: ").append(data.getLigneOriginale()).append("\n\n");
            }
        }
        
        if (errorCount == 0) {
            resume.append("Aucune erreur détectée.\n");
        }
        
        resume.append("\n🎯 Résumé généré le ").append(new Date()).append("\n");
        
        return resume.toString();
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
}
