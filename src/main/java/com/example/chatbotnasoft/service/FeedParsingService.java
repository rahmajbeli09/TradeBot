package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.ParsedFeedGroup;
import com.example.chatbotnasoft.dto.ParsingResult;
import com.example.chatbotnasoft.dto.RawFeedLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeedParsingService {

    private static final String FIELD_SEPARATOR = ";";

    public ParsingResult parseAndGroupLines(List<RawFeedLine> lines) {
        log.info("🔍 Début du parsing et regroupement de {} lignes", lines.size());
        
        Map<String, ParsedFeedGroup> groupsByMsgType = new HashMap<>();
        List<String> parsingErrors = new ArrayList<>();
        int validLinesProcessed = 0;
        String sourceFileName = null;

        for (RawFeedLine line : lines) {
            try {
                String msgType = extractMsgType(line);
                
                if (msgType == null || msgType.trim().isEmpty()) {
                    String error = String.format("Msg-type vide ou invalide - Ligne %d: '%s'", 
                            line.getLineNumber(), line.getTrimmedContent());
                    parsingErrors.add(error);
                    log.warn("⚠️ {}", error);
                    continue;
                }

                // Ajouter la ligne au groupe approprié
                groupsByMsgType.computeIfAbsent(msgType, k -> {
                    log.info("📋 Création d'un nouveau groupe pour msg-type: '{}'", msgType);
                    return new ParsedFeedGroup(msgType, new ArrayList<>());
                }).addLine(line);

                validLinesProcessed++;
                log.debug("✅ Ligne {} ajoutée au groupe '{}'", line.getLineNumber(), msgType);
                
                if (sourceFileName == null) {
                    sourceFileName = line.getSourceFileName();
                }

            } catch (Exception e) {
                String error = String.format("Erreur de parsing - Ligne %d: '%s' - Erreur: %s", 
                        line.getLineNumber(), line.getTrimmedContent(), e.getMessage());
                parsingErrors.add(error);
                log.error("❌ {}", error);
            }
        }

        // Log des résultats
        logParsingSummary(groupsByMsgType, parsingErrors, lines.size(), validLinesProcessed);

        return new ParsingResult(groupsByMsgType, parsingErrors, lines.size(), 
                validLinesProcessed, sourceFileName);
    }

    private String extractMsgType(RawFeedLine line) {
        String content = line.getTrimmedContent();
        
        // Découper la ligne sur le séparateur ;
        String[] fields = content.split(FIELD_SEPARATOR);
        
        // Vérifier qu'il y a au moins 2 champs
        if (fields.length < 2) {
            throw new IllegalArgumentException(String.format(
                    "Ligne %d: nombre de champs insuffisant (%d trouvé, minimum 2 requis)", 
                    line.getLineNumber(), fields.length));
        }

        // Extraire le deuxième champ (index 1)
        String msgType = fields[1].trim();
        
        log.debug("🔧 Extraction msg-type - Ligne {}: '{}' -> '{}'", 
                line.getLineNumber(), content, msgType);
        
        return msgType;
    }

    private void logParsingSummary(Map<String, ParsedFeedGroup> groupsByMsgType, 
                                 List<String> parsingErrors, int totalLines, int validLines) {
        
        log.info("📊 Résumé du parsing:");
        log.info("   • Lignes totales traitées: {}", totalLines);
        log.info("   • Lignes valides: {}", validLines);
        log.info("   • Erreurs de parsing: {}", parsingErrors.size());
        log.info("   • Groupes créés: {}", groupsByMsgType.size());
        
        // Détail des groupes
        groupsByMsgType.entrySet().stream()
                .sorted(Map.Entry.<String, ParsedFeedGroup>comparingByValue(
                        (g1, g2) -> Integer.compare(g2.getTotalLines(), g1.getTotalLines())))
                .forEach(entry -> {
                    ParsedFeedGroup group = entry.getValue();
                    log.info("   • Groupe '{}': {} lignes", entry.getKey(), group.getTotalLines());
                });

        // Détail des erreurs (limité aux 5 premières)
        if (!parsingErrors.isEmpty()) {
            log.warn("⚠️ Erreurs de parsing (5 premières sur {}):", parsingErrors.size());
            parsingErrors.stream().limit(5).forEach(error -> log.warn("   • {}", error));
            if (parsingErrors.size() > 5) {
                log.warn("   • ... et {} autres erreurs", parsingErrors.size() - 5);
            }
        }
    }

    public boolean isValidFeedLine(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String[] fields = content.split(FIELD_SEPARATOR);
        return fields.length >= 2 && !fields[1].trim().isEmpty();
    }
}
