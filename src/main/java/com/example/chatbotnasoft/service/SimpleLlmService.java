package com.example.chatbotnasoft.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SimpleLlmService {

    public String generate(String question, String context) {
        log.info("🤖 SimpleLLM: génération réponse factuelle");
        log.info("🔍 QUESTION: {}", question);
        log.info("📝 CONTEXT: {}", context);
        
        // 1. Validation des questions ambiguës
        if (isQuestionAmbigue(question)) {
            return "Désolé, la question est trop ambiguë pour fournir une réponse fiable.";
        }
        
        // 2. Validation des questions comparatives
        if (isQuestionComparative(question)) {
            return "Désolé, les questions comparatives nécessitent plusieurs msgTypes spécifiques pour être traitées.";
        }
        
        // Extraire le msgType et le champ de la question
        String msgType = extractMsgType(question);
        String champRecherche = extractChampRecherche(question);
        
        log.info("📍 MSGTYPE EXTRACTION: '{}'", msgType);
        log.info("🏷️ CHAMP RECHERCHE: '{}'", champRecherche);
        
        // 3. Analyser le contexte pour trouver la réponse
        Map<String, String> mapping = extractMappingFromContext(context, msgType);
        
        log.info("🗺️ MAPPING EXTRACTED: {}", mapping);
        
        if (mapping == null) {
            return "Désolé, je n'ai pas trouvé d'information pour cette question.";
        }
        
        // 4. Validation intra-document (champ précis)
        if (champRecherche != null) {
            String champTrouve = findChampCorrespondant(mapping, champRecherche);
            log.info("🎯 CHAMP TROUVÉ: '{}' pour recherche '{}'", champTrouve, champRecherche);
            
            if (champTrouve != null) {
                // Répondre UNIQUEMENT au champ demandé
                return String.format("Le %s du msgType %s représente : %s", 
                        champRecherche, msgType, mapping.get(champTrouve));
            } else {
                // Champ n'existe pas dans le mapping
                return "Information non disponible pour ce champ.";
            }
        }
        
        // 5. Si pas de champ spécifique, donner une vue d'ensemble
        return String.format("Pour le msgType %s : %s", msgType, formatMapping(mapping));
    }
    
    private String findChampCorrespondant(Map<String, String> mapping, String champRecherche) {
        log.info("🔍 Looking for '{}' in mapping keys: {}", champRecherche, mapping.keySet());
        
        // 1. Correspondance exacte
        for (String champ : mapping.keySet()) {
            if (champ.equalsIgnoreCase(champRecherche)) {
                log.info("✅ Exact match found: '{}'", champ);
                return champ;
            }
        }
        
        // 2. Correspondance partielle (contient)
        for (String champ : mapping.keySet()) {
            if (champ.toLowerCase().contains(champRecherche.toLowerCase()) ||
                champRecherche.toLowerCase().contains(champ.toLowerCase())) {
                log.info("✅ Partial match found: '{}' contains '{}'", champ, champRecherche);
                return champ;
            }
        }
        
        // 3. Correspondance sémantique pour "identifiant"
        if (champRecherche.toLowerCase().contains("identifiant")) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String valeur = entry.getValue().toLowerCase();
                if (valeur.contains("id") || valeur.contains("référence") || 
                    valeur.contains("unique") || valeur.contains("identifiant")) {
                    log.info("✅ Semantic match for identifiant: '{}' = '{}'", entry.getKey(), entry.getValue());
                    return entry.getKey();
                }
            }
        }
        
        // 4. Correspondance sémantique pour "montant"
        if (champRecherche.toLowerCase().contains("montant")) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String valeur = entry.getValue().toLowerCase();
                if (valeur.contains("prix") || valeur.contains("taux") || 
                    valeur.contains("coût") || valeur.contains("valeur") || valeur.contains("montant")) {
                    log.info("✅ Semantic match for montant: '{}' = '{}'", entry.getKey(), entry.getValue());
                    return entry.getKey();
                }
            }
        }
        
        // 5. Correspondance par numéro de champ
        if (champRecherche.toLowerCase().startsWith("champ")) {
            String numero = champRecherche.replaceAll("[^0-9]", "");
            if (!numero.isEmpty()) {
                String champNumerique = "Champ " + numero;
                if (mapping.containsKey(champNumerique)) {
                    log.info("✅ Numeric match found: '{}'", champNumerique);
                    return champNumerique;
                }
            }
        }
        
        log.info("❌ No match found for '{}'", champRecherche);
        return null;
    }
    
    private boolean isQuestionAmbigue(String question) {
        String lowerQuestion = question.toLowerCase();
        
        // Questions vraiment trop générales (sans msgType ET sans champ)
        if (lowerQuestion.contains("information sur") || 
            lowerQuestion.contains("détaille") ||
            lowerQuestion.contains("tout") ||
            lowerQuestion.contains("général") ||
            lowerQuestion.contains("global")) {
            
            // Accepter si msgType ou champ est mentionné
            if (lowerQuestion.contains("msgtype") || containsChampKeyword(lowerQuestion)) {
                return false; // Pas ambiguë
            }
            return true; // Ambiguë
        }
        
        // Questions sans aucun identifiant
        if (!lowerQuestion.contains("msgtype") && 
            !containsChampKeyword(lowerQuestion)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isQuestionComparative(String question) {
        String lowerQuestion = question.toLowerCase();
        
        // Mots-clés comparatifs
        return lowerQuestion.contains("différence") ||
               lowerQuestion.contains("comparaison") ||
               lowerQuestion.contains("versus") ||
               lowerQuestion.contains("contre") ||
               lowerQuestion.contains("plutôt que");
    }
    
    private boolean containsChampKeyword(String question) {
        return question.contains("champ") ||
               question.contains("identifiant") ||
               question.contains("montant") ||
               question.contains("type") ||
               question.contains("statut") ||
               question.contains("quantité");
    }
    
    private String extractMsgType(String question) {
        Pattern pattern = Pattern.compile("msgType\\s+(\\w+)");
        Matcher matcher = pattern.matcher(question);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractChampRecherche(String question) {
        log.info(" Extracting champ from question: '{}'", question);
        
        String lowerQuestion = question.toLowerCase();
        
        // Recherche exacte des termes complets en premier
        if (lowerQuestion.contains("identifiant unique")) {
            log.info(" Found 'identifiant unique'");
            return "Identifiant unique";
        }
        if (lowerQuestion.contains("montant de l'opération")) {
            log.info(" Found 'montant de l'opération'");
            return "Montant de l'opération";
        }
        if (lowerQuestion.contains("type de message")) {
            log.info(" Found 'type de message'");
            return "Type de message";
        }
        
        // Recherche des mots-clés simples
        if (lowerQuestion.contains("montant")) {
            log.info(" Found 'montant'");
            return "Montant de l'opération";
        }
        if (lowerQuestion.contains("identifiant")) {
            log.info(" Found 'identifiant'");
            return "Identifiant unique";
        }
        if (lowerQuestion.contains("type")) {
            log.info(" Found 'type'");
            return "Type de message";
        }
        if (lowerQuestion.contains("statut")) {
            log.info(" Found 'statut'");
            return "Statut";
        }
        if (lowerQuestion.contains("quantité")) {
            log.info(" Found 'quantité'");
            return "Quantité";
        }
        
        // Fallback: chercher "champ X"
        Pattern pattern = Pattern.compile("champ\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            String champ = "Champ " + matcher.group(1);
            log.info(" Fallback to regex: '{}'", champ);
            return champ;
        }
        
        log.info("❌ No champ found");
        return null;
    }
    
    private String extractChamp(String question) {
        Pattern pattern = Pattern.compile("champ\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(question);
        return matcher.find() ? "Champ " + matcher.group(1) : null;
    }
    
    private Map<String, String> extractMappingFromContext(String context, String msgType) {
        // Parser le contexte pour extraire le mapping du msgType demandé
        String[] lines = context.split("\n");
        Map<String, String> targetMapping = new java.util.HashMap<>();
        String currentMsgType = null;
        boolean foundTarget = false;
        
        for (String line : lines) {
            line = line.trim();
            
            // Format: "Contexte principal (msgType 53) :"
            if (line.contains("msgType")) {
                Pattern pattern = Pattern.compile("msgType\\s+(\\w+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    currentMsgType = matcher.group(1);
                    log.info("🔍 Found msgType in context: '{}' (looking for '{}')", currentMsgType, msgType);
                    
                    if (currentMsgType.equals(msgType)) {
                        foundTarget = true;
                        log.info("✅ Found target msgType {}, collecting fields...", msgType);
                    } else {
                        foundTarget = false;
                    }
                }
            } else if (line.startsWith("-") && foundTarget) {
                // Extraire champ et signification - format: "- Champ 4 : Identifiant secondaire..."
                String[] parts = line.substring(1).trim().split(" : ", 2);
                if (parts.length == 2) {
                    targetMapping.put(parts[0].trim(), parts[1].trim());
                    log.info("📝 Added to mapping: '{}' = '{}'", parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        log.info("📋 Final mapping for msgType {}: {} (found: {})", msgType, targetMapping, !targetMapping.isEmpty());
        return targetMapping.isEmpty() ? null : targetMapping;
    }
    
    private String formatMapping(Map<String, String> mapping) {
        StringBuilder sb = new StringBuilder();
        mapping.forEach((champ, signification) -> 
                sb.append(champ).append(" = ").append(signification).append(", ")
        );
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2); // Enlever la dernière virgule
        }
        return sb.toString();
    }
}
