package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.AnonymizedLine;
import com.example.chatbotnasoft.dto.AnonymizationResult;
import com.example.chatbotnasoft.dto.ParsedFeedGroup;
import com.example.chatbotnasoft.dto.RawFeedLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnonymizationService {
    private static final String FIELD_SEPARATOR = ";";
    private static final String ANONYMIZED_VALUE = "xxxxx";

    private final FeedDetectionService feedDetectionService;

    public AnonymizationResult processGroups(Map<String, ParsedFeedGroup> groupsByMsgType) {
        log.info("🔍 Début de l'anonymisation pour {} groupes", groupsByMsgType.size());
        
        Map<String, List<AnonymizedLine>> resultsByMsgType = new HashMap<>();
        List<String> unknownMsgTypes = new ArrayList<>();
        List<String> knownMsgTypes = new ArrayList<>();
        int totalLinesProcessed = 0;
        String sourceFileName = null;

        // Traiter chaque groupe de msg-type
        for (Map.Entry<String, ParsedFeedGroup> entry : groupsByMsgType.entrySet()) {
            String msgType = entry.getKey();
            ParsedFeedGroup group = entry.getValue();
            
            totalLinesProcessed += group.getTotalLines();
            
            if (sourceFileName == null) {
                sourceFileName = group.getSourceFileName();
            }

            // Vérifier si le msg-type est connu
            boolean isKnown = feedDetectionService.isMsgTypeKnown(msgType);
            
            if (isKnown) {
                log.info("✅ Msg-type '{}' connu - pas d'anonymisation", msgType);
                knownMsgTypes.add(msgType);
                
                // Créer les lignes non anonymisées
                List<AnonymizedLine> nonAnonymizedLines = group.getLines().stream()
                        .map(line -> new AnonymizedLine(
                                line.getTrimmedContent(),
                                line.getTrimmedContent(), // Ligne inchangée
                                msgType,
                                line.getLineNumber(),
                                line.getSourceFileName(),
                                false // Non anonymisée
                        ))
                        .collect(Collectors.toList());
                
                resultsByMsgType.put(msgType, nonAnonymizedLines);
                
            } else {
                log.info("❌ Msg-type '{}' inconnu - anonymisation des champs", msgType);
                unknownMsgTypes.add(msgType);
                
                // Anonymiser les lignes de ce groupe
                List<AnonymizedLine> anonymizedLines = anonymizeGroup(group);
                resultsByMsgType.put(msgType, anonymizedLines);
                
                log.info("🔒 {} lignes anonymisées pour le msg-type '{}'", 
                        anonymizedLines.size(), msgType);
            }
        }

        // Log du résumé
        logAnonymizationSummary(unknownMsgTypes, knownMsgTypes, totalLinesProcessed, resultsByMsgType);

        return new AnonymizationResult(resultsByMsgType, unknownMsgTypes, knownMsgTypes, 
                totalLinesProcessed, sourceFileName);
    }

    private List<AnonymizedLine> anonymizeGroup(ParsedFeedGroup group) {
        String msgType = group.getMsgType();
        
        return group.getLines().stream()
                .map(line -> {
                    String anonymizedLine = anonymizeLine(line.getTrimmedContent());
                    
                    log.debug("🔒 Anonymisation - Ligne {}: '{}' -> '{}'", 
                            line.getLineNumber(), line.getTrimmedContent(), anonymizedLine);
                    
                    return new AnonymizedLine(
                            line.getTrimmedContent(),
                            anonymizedLine,
                            msgType,
                            line.getLineNumber(),
                            line.getSourceFileName(),
                            true // Anonymisée
                    );
                })
                .collect(Collectors.toList());
    }

    private String anonymizeLine(String originalLine) {
        // Découper la ligne sur le séparateur ;
        String[] fields = originalLine.split(FIELD_SEPARATOR, -1); // -1 pour garder les champs vides
        
        if (fields.length < 2) {
            log.warn("⚠️ Ligne mal formée, moins de 2 champs: '{}'", originalLine);
            return originalLine; // Retourner la ligne originale
        }

        // Conserver les 2 premiers champs intacts
        String[] anonymizedFields = new String[fields.length];
        anonymizedFields[0] = fields[0]; // 1er champ intact
        anonymizedFields[1] = fields[1]; // 2ème champ (msg-type) intact

        // Anonymiser tous les champs à partir du 3ème (index 2)
        for (int i = 2; i < fields.length; i++) {
            anonymizedFields[i] = ANONYMIZED_VALUE;
        }

        // Recomposer la ligne anonymisée
        return String.join(FIELD_SEPARATOR, anonymizedFields);
    }

    private void logAnonymizationSummary(List<String> unknownMsgTypes, List<String> knownMsgTypes,
                                    int totalLinesProcessed, Map<String, List<AnonymizedLine>> resultsByMsgType) {
        
        log.info("📊 Résumé de l'anonymisation:");
        log.info("   • Lignes totales traitées: {}", totalLinesProcessed);
        log.info("   • Msg-types inconnus: {} ({})", unknownMsgTypes.size(), unknownMsgTypes);
        log.info("   • Msg-types connus: {} ({})", knownMsgTypes.size(), knownMsgTypes);
        
        // Détail des msg-types inconnus avec nombre de lignes
        unknownMsgTypes.forEach(msgType -> {
            int lineCount = resultsByMsgType.get(msgType).size();
            log.info("   • Msg-type inconnu '{}': {} lignes anonymisées", msgType, lineCount);
        });

        // Statistiques
        int anonymizedLinesCount = unknownMsgTypes.stream()
                .mapToInt(msgType -> resultsByMsgType.get(msgType).size())
                .sum();
        
        log.info("   • Lignes anonymisées: {}", anonymizedLinesCount);
        log.info("   • Lignes non anonymisées: {}", totalLinesProcessed - anonymizedLinesCount);
        
        if (totalLinesProcessed > 0) {
            double anonymizationRate = (anonymizedLinesCount * 100.0) / totalLinesProcessed;
            log.info("   • Taux d'anonymisation: {:.1f}%", anonymizationRate);
        }
    }

    public boolean isAnonymizedLine(String originalLine, String anonymizedLine) {
        return !originalLine.equals(anonymizedLine);
    }
}
