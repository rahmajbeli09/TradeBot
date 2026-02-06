package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.ChampDetail;
import com.example.chatbotnasoft.dto.ResumeDataAmeliore;
import com.example.chatbotnasoft.dto.ResumeResponseAmeliore;
import com.example.chatbotnasoft.dto.ResumeStatistiquesAmeliorees;
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
 * Service amélioré pour générer des résumés structurés complets des fichiers FEED
 */
@Service
@Slf4j
public class ResumeFeedAmelioreService {

    @Autowired
    private FeedMappingRepository feedMappingRepository;

    /**
     * Génère un résumé complet et amélioré d'un fichier FEED
     * @param filePath Chemin du fichier FEED à analyser
     * @return ResumeResponseAmeliore avec les données structurées et le résumé textuel amélioré
     */
    public ResumeResponseAmeliore generateResumeAmeliore(String filePath) {
        log.info("📊 Début de la génération de résumé amélioré pour: {}", filePath);
        
        List<ResumeDataAmeliore> resumeDataList = new ArrayList<>();
        String fileName = extractFileName(filePath);
        
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
                
                ResumeDataAmeliore resumeData = processLineToResumeDataAmeliore(ligne, numeroLigne);
                resumeDataList.add(resumeData);
            }
            
            log.info("✅ Traitement amélioré terminé: {} lignes analysées", resumeDataList.size());
            
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Erreur de lecture du fichier: " + e.getMessage(), e);
        }
        
        // Générer le résumé textuel amélioré
        String resumeTexteAmeliore = generateResumeTexteAmeliore(resumeDataList, fileName);
        
        return new ResumeResponseAmeliore(fileName, resumeDataList, resumeTexteAmeliore);
    }

    /**
     * Traite une ligne individuelle pour créer un ResumeDataAmeliore
     * @param ligne Ligne FEED à traiter
     * @param numeroLigne Numéro de la ligne pour le logging
     * @return ResumeDataAmeliore avec les données structurées améliorées
     */
    private ResumeDataAmeliore processLineToResumeDataAmeliore(String ligne, int numeroLigne) {
        try {
            // Extraire les champs séparés par ";"
            String[] champs = ligne.split(";");
            
            // Vérifier qu'on a au moins 2 champs pour extraire le msgType
            if (champs.length < 2) {
                String erreur = "Ligne " + numeroLigne + ": Moins de 2 champs trouvés";
                log.warn("⚠️ {}", erreur);
                return new ResumeDataAmeliore("", ligne, erreur);
            }
            
            // Extraire le msgType (2ème champ)
            String msgType = champs[1].trim();
            log.debug("🔍 MsgType extrait: '{}' pour ligne {}", msgType, numeroLigne);
            
            // Chercher le mapping dans MongoDB
            Optional<FeedMapping> mappingOpt = feedMappingRepository.findByMsgType(msgType);
            
            if (mappingOpt.isEmpty()) {
                String erreur = "Aucun mapping trouvé pour le msgType: " + msgType;
                log.warn("⚠️ Ligne {}: {}", numeroLigne, erreur);
                return new ResumeDataAmeliore(msgType, ligne, erreur);
            }
            
            // Créer les maps et la liste de champs détaillés
            Map<String, String> valeursMap = createValeursMap(champs);
            Map<String, String> valeursAnonymiseesMap = createValeursAnonymiseesMap(champs);
            List<ChampDetail> mappingComplet = createMappingComplet(champs, mappingOpt.get().getMapping());
            
            log.debug("✅ Ligne {} traitée avec succès - {} champs", numeroLigne, valeursMap.size());
            return new ResumeDataAmeliore(msgType, mappingComplet, valeursMap, valeursAnonymiseesMap, ligne);
            
        } catch (Exception e) {
            String erreur = "Erreur lors du traitement de la ligne " + numeroLigne + ": " + e.getMessage();
            log.error("❌ {}", erreur, e);
            return new ResumeDataAmeliore("", ligne, erreur);
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
     * Crée une map des valeurs anonymisées
     * @param champs Tableau des champs de la ligne
     * @return Map avec "Champ i" comme clé et la valeur anonymisée comme valeur
     */
    private Map<String, String> createValeursAnonymiseesMap(String[] champs) {
        Map<String, String> valeursAnonymiseesMap = new LinkedHashMap<>();
        
        for (int i = 0; i < champs.length; i++) {
            String champKey = "Champ " + (i + 1);
            String champValue = champs[i].trim();
            String valeurAnonymisee = anonymiserValeur(champValue, i + 1);
            valeursAnonymiseesMap.put(champKey, valeurAnonymisee);
        }
        
        return valeursAnonymiseesMap;
    }

    /**
     * Crée la liste complète des champs détaillés avec significations
     * @param champs Tableau des champs de la ligne
     * @param mapping Mapping trouvé dans MongoDB
     * @return Liste de ChampDetail avec tous les champs
     */
    private List<ChampDetail> createMappingComplet(String[] champs, Map<String, String> mapping) {
        List<ChampDetail> mappingComplet = new ArrayList<>();
        
        for (int i = 0; i < champs.length; i++) {
            String champKey = "Champ " + (i + 1);
            String champValue = champs[i].trim();
            String signification = mapping.getOrDefault(champKey, "Inconnu");
            String valeurAnonymisee = anonymiserValeur(champValue, i + 1);
            
            ChampDetail champDetail = new ChampDetail(champKey, signification, champValue, valeurAnonymisee);
            mappingComplet.add(champDetail);
        }
        
        return mappingComplet;
    }

    /**
     * Anonymise une valeur selon des règles prédéfinies
     * @param valeur Valeur à anonymiser
     * @param champNum Numéro du champ (pour appliquer des règles spécifiques)
     * @return Valeur anonymisée
     */
    private String anonymiserValeur(String valeur, int champNum) {
        if (valeur == null || valeur.trim().isEmpty()) {
            return valeur;
        }
        
        String upperValeur = valeur.toUpperCase().trim();
        
        // Règles spécifiques par numéro de champ
        switch (champNum) {
            case 1: // Type d'enregistrement - garder tel quel
                return valeur;
            case 2: // Code de statut - garder tel quel
                return valeur;
            case 3: // Date - masquer partiellement
                if (upperValeur.matches("\\d{8}")) {
                    return valeur.substring(0, 4) + "**/**";
                }
                break;
            case 4: // Heure - masquer partiellement
                if (upperValeur.matches("\\d{6}")) {
                    return valeur.substring(0, 2) + "***";
                }
                break;
            case 5: // Référence opération - anonymiser complètement
                if (upperValeur.matches("[A-Z0-9]{8,}")) {
                    return "REF_" + upperValeur.substring(0, 3) + "***";
                }
                break;
        }
        
        // Règles générales de sensibilité
        if (upperValeur.matches("[A-Z0-9]{8,}")) {
            // Références uniques longues
            return upperValeur.substring(0, 3) + "***" + upperValeur.substring(upperValeur.length() - 2);
        }
        
        if (upperValeur.matches("\\d{10,}")) {
            // Numéros longs
            return "***" + upperValeur.substring(upperValeur.length() - 4);
        }
        
        if (upperValeur.matches("^[A-Z]{3,}$")) {
            // Codes alphabétiques courts
            return upperValeur.substring(0, 1) + "**";
        }
        
        return valeur;
    }

    /**
     * Génère un résumé textuel amélioré et complet
     * @param resumeDataList Liste des données résumées améliorées
     * @param fileName Nom du fichier traité
     * @return Résumé textuel formaté et complet
     */
    private String generateResumeTexteAmeliore(List<ResumeDataAmeliore> resumeDataList, String fileName) {
        StringBuilder resume = new StringBuilder();
        
        // En-tête du résumé
        resume.append("📊 RÉSUMÉ AMÉLIORÉ DU FICHIER FEED\n");
        resume.append("=====================================\n");
        resume.append("Fichier: ").append(fileName).append("\n");
        resume.append("Généré le: ").append(new Date()).append("\n\n");
        
        // Statistiques détaillées
        ResumeStatistiquesAmeliorees stats = new ResumeStatistiquesAmeliorees(resumeDataList);
        resume.append("📈 STATISTIQUES DÉTAILLÉES\n");
        resume.append("============================\n");
        resume.append("• Lignes totales traitées: ").append(stats.getTotalLignes()).append("\n");
        resume.append("• Lignes avec mapping complet (≥90%): ").append(stats.getLignesAvecMappingComplet()).append("\n");
        resume.append("• Lignes avec mapping partiel: ").append(stats.getLignesAvecMappingPartiel()).append("\n");
        resume.append("• Lignes sans mapping: ").append(stats.getLignesSansMapping()).append("\n");
        resume.append("• Taux de succès global: ").append(String.format("%.1f", stats.getTauxSuccesGlobal())).append("%\n");
        resume.append("• Taux de mapping complet: ").append(String.format("%.1f", stats.getTauxMappingComplet())).append("%\n");
        resume.append("• Types de messages différents: ").append(stats.getMsgTypesUniques().size()).append("\n");
        resume.append("• Total de champs différents: ").append(stats.getTotalChampsDifferents()).append("\n\n");
        
        // Répartition par msgType avec moyennes
        resume.append("🏷️ RÉPARTITION DÉTAILLÉE PAR MSG-TYPE\n");
        resume.append("=====================================\n");
        Map<String, Integer> msgTypeCount = stats.getMsgTypeCount();
        Map<String, Double> moyennesChamps = stats.getMoyenneChampsParMsgType();
        Map<String, Double> tauxCompletion = stats.getTauxCompletionParMsgType();
        List<String> sortedMsgTypes = new ArrayList<>(msgTypeCount.keySet());
        Collections.sort(sortedMsgTypes);
        
        for (String msgType : sortedMsgTypes) {
            int count = msgTypeCount.get(msgType);
            double moyenneChamps = moyennesChamps.get(msgType);
            double completion = tauxCompletion.get(msgType);
            
            resume.append("📋 MsgType: ").append(msgType).append("\n");
            resume.append("   • Nombre de lignes: ").append(count).append("\n");
            resume.append("   • Moyenne de champs: ").append(String.format("%.1f", moyenneChamps)).append("\n");
            resume.append("   • Taux de complétude: ").append(String.format("%.1f", completion)).append("%\n");
        }
        resume.append("\n");
        
        // Champs les plus fréquents
        resume.append("🔢 CHAMPS LES PLUS FRÉQUENTS\n");
        resume.append("=============================\n");
        Map<String, Integer> champsFreq = stats.getChampsPlusFrequents();
        List<Map.Entry<String, Integer>> sortedChamps = new ArrayList<>(champsFreq.entrySet());
        sortedChamps.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        int topChamps = Math.min(10, sortedChamps.size());
        for (int i = 0; i < topChamps; i++) {
            Map.Entry<String, Integer> entry = sortedChamps.get(i);
            double pourcentage = (entry.getValue() * 100.0) / stats.getTotalLignes();
            resume.append((i + 1)).append(". ").append(entry.getKey())
                  .append(": ").append(entry.getValue()).append(" occurrences (")
                  .append(String.format("%.1f", pourcentage)).append("%)\n");
        }
        resume.append("\n");
        
        // Détails complets par msgType
        resume.append("🔍 DÉTAILS COMPLETS PAR MSG-TYPE\n");
        resume.append("=================================\n");
        
        // Grouper les lignes par msgType
        Map<String, List<ResumeDataAmeliore>> groupedByMsgType = new HashMap<>();
        for (ResumeDataAmeliore data : resumeDataList) {
            if (data.isMappingTrouve()) {
                String msgType = data.getMsgType();
                groupedByMsgType.computeIfAbsent(msgType, k -> new ArrayList<>()).add(data);
            }
        }
        
        // Générer les détails complets pour chaque msgType
        for (String msgType : sortedMsgTypes) {
            List<ResumeDataAmeliore> msgTypeLines = groupedByMsgType.get(msgType);
            if (msgTypeLines != null && !msgTypeLines.isEmpty()) {
                resume.append("📋 MsgType: ").append(msgType).append(" (").append(msgTypeLines.size()).append(" lignes)\n");
                
                // Afficher toutes les lignes avec tous leurs champs
                for (int i = 0; i < Math.min(5, msgTypeLines.size()); i++) {
                    ResumeDataAmeliore data = msgTypeLines.get(i);
                    resume.append("   ").append(i + 1).append(". [").append(data.getTotalChamps()).append(" champs] ");
                    
                    // Afficher TOUS les champs avec leur nom/signification/valeur
                    List<ChampDetail> champs = data.getMappingComplet();
                    for (int j = 0; j < champs.size(); j++) {
                        ChampDetail champ = champs.get(j);
                        resume.append(champ.getChamp()).append("(");
                        resume.append(champ.getSignification()).append("):");
                        resume.append(champ.getValeur());
                        
                        if (champ.isValeurSensible()) {
                            resume.append("[*]");
                        }
                        
                        if (j < champs.size() - 1) {
                            resume.append(" | ");
                        }
                    }
                    resume.append("\n");
                }
                
                if (msgTypeLines.size() > 5) {
                    resume.append("   ... et ").append(msgTypeLines.size() - 5).append(" autres lignes similaires\n");
                }
                resume.append("\n");
            }
        }
        
        // Lignes avec erreurs
        resume.append("❌ LIGNES AVEC ERREURS\n");
        resume.append("========================\n");
        int errorCount = 0;
        for (ResumeDataAmeliore data : resumeDataList) {
            if (!data.isMappingTrouve()) {
                errorCount++;
                resume.append(errorCount).append(". ").append(data.getErreur()).append("\n");
                resume.append("   Ligne: ").append(data.getLigneOriginale()).append("\n\n");
            }
        }
        
        if (errorCount == 0) {
            resume.append("Aucune erreur détectée.\n");
        }
        
        resume.append("\n🎯 Résumé amélioré généré avec succès\n");
        
        return resume.toString();
    }

    /**
     * Extrait le nom du fichier à partir du chemin complet
     * @param filePath Chemin complet du fichier
     * @return Nom du fichier seul
     */
    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "Fichier inconnu";
        }
        
        // Gérer les séparateurs Windows et Unix
        String separator = filePath.contains("\\") ? "\\\\" : "/";
        String[] parts = filePath.split(separator);
        return parts[parts.length - 1];
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
