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
        
        // Extraire le msgType et le champ de la question
        String msgType = extractMsgType(question);
        String champRecherche = extractChampRecherche(question);
        
        log.info("📍 MSGTYPE EXTRACTION: '{}'", msgType);
        log.info("🏷️ CHAMP RECHERCHE: '{}'", champRecherche);
        
        // Analyser le contexte pour trouver la réponse
        Map<String, String> mapping = extractMappingFromContext(context, msgType);
        
        log.info("🗺️ MAPPING EXTRACTED: {}", mapping);
        
        if (mapping == null) {
            return "Désolé, je n'ai pas trouvé d'information pour cette question.";
        }
        
        // Chercher une correspondance sémantique
        String champTrouve = findChampCorrespondant(mapping, champRecherche);
        
        log.info("🎯 CHAMP TROUVÉ: '{}' pour recherche '{}'", champTrouve, champRecherche);
        
        if (champTrouve != null) {
            return String.format("Le %s du msgType %s représente : %s", 
                    champRecherche, msgType, mapping.get(champTrouve));
        }
        
        // Si pas de champ spécifique, donner une vue d'ensemble
        return String.format("Pour le msgType %s : %s", msgType, formatMapping(mapping));
    }
    
    private String findChampCorrespondant(Map<String, String> mapping, String champRecherche) {
        // Correspondances exactes
        for (String champ : mapping.keySet()) {
            if (champ.toLowerCase().contains(champRecherche.toLowerCase()) ||
                champRecherche.toLowerCase().contains(champ.toLowerCase())) {
                return champ;
            }
        }
        
        // Correspondances sémantiques pour "montant"
        if (champRecherche.toLowerCase().contains("montant")) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String valeur = entry.getValue().toLowerCase();
                if (valeur.contains("prix") || valeur.contains("taux") || 
                    valeur.contains("coût") || valeur.contains("valeur")) {
                    return entry.getKey();
                }
            }
        }
        
        // Correspondances sémantiques pour "identifiant"
        if (champRecherche.toLowerCase().contains("identifiant")) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String valeur = entry.getValue().toLowerCase();
                if (valeur.contains("id") || valeur.contains("référence") || 
                    valeur.contains("unique")) {
                    return entry.getKey();
                }
            }
        }
        
        return null;
    }
    
    private String extractMsgType(String question) {
        Pattern pattern = Pattern.compile("msgType\\s+(\\w+)");
        Matcher matcher = pattern.matcher(question);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractChampRecherche(String question) {
        log.info("🔍 Extracting champ from question: '{}'", question);
        
        // Chercher des termes sémantiques complets en premier
        String lowerQuestion = question.toLowerCase();
        if (lowerQuestion.contains("montant de l'opération")) {
            log.info("✅ Found 'montant de l'opération'");
            return "Montant de l'opération";
        }
        if (lowerQuestion.contains("montant")) {
            log.info("✅ Found 'montant'");
            return "Montant de l'opération";
        }
        if (lowerQuestion.contains("identifiant")) {
            log.info("✅ Found 'identifiant'");
            return "Identifiant unique";
        }
        if (lowerQuestion.contains("type")) {
            log.info("✅ Found 'type'");
            return "Type de message";
        }
        
        // Chercher "champ X" seulement si rien trouvé avant
        Pattern pattern = Pattern.compile("champ\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            String champ = "Champ " + matcher.group(1);
            log.info("⚠️ Fallback to regex: '{}'", champ);
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
            if (line.startsWith("MsgType")) {
                // Extraire le msgType - format: "MsgType 53 (score: 0,79) :"
                Pattern pattern = Pattern.compile("MsgType\\s+(\\w+)\\s+\\(score:");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    currentMsgType = matcher.group(1);
                    log.info("🔍 Found msgType in context: '{}' (looking for '{}')", currentMsgType, msgType);
                    
                    if (currentMsgType.equals(msgType)) {
                        foundTarget = true;
                        log.info("� Found target msgType {}, collecting fields...", msgType);
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
