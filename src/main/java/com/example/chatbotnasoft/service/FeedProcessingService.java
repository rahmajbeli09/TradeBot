package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.AnonymizationResult;
import com.example.chatbotnasoft.dto.LLMAnalysisResult;
import com.example.chatbotnasoft.dto.ParsingResult;
import com.example.chatbotnasoft.dto.RawFeedLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedProcessingService {

    private final FileReadingService fileReadingService;
    private final FileProcessingService fileProcessingService;
    private final FeedParsingService feedParsingService;
    private final AnonymizationService anonymizationService;
    private final LLMService llmService;
    private final FeedMappingService feedMappingService;

    public void processFeedFile(Path filePath) {
        log.info("🔍 Vérification du fichier: {}", filePath.getFileName());
        
        if (!fileReadingService.isValidFeedFile(filePath)) {
            log.warn("❌ Fichier non valide pour le traitement: {}", filePath.getFileName());
            return;
        }

        log.info("✅ Fichier valide, début du traitement: {}", filePath.getFileName());

        try {
            // Compter les lignes d'abord
            log.info("📊 Comptage des lignes en cours...");
            long totalLines = fileReadingService.countLines(filePath);
            log.info("📈 Le fichier contient {} lignes à traiter", totalLines);

            // Traiter les lignes avec streaming
            AtomicInteger processedLines = new AtomicInteger(0);
            log.info("🚀 Début de la lecture streaming des lignes...");
            
            List<RawFeedLine> allLines = new ArrayList<>();
            
            try (Stream<RawFeedLine> lines = fileReadingService.readFileLines(filePath)) {
                lines.forEach(line -> {
                    allLines.add(line);
                    int current = processedLines.incrementAndGet();
                    
                    // Log de progression toutes les 1000 lignes ou pour les petits fichiers
                    if (current % 1000 == 0 || current == totalLines || current <= 10) {
                        log.info("📖 Progression: {}/{} lignes traitées ({:.1f}%) - Contenu: '{}'", 
                                current, totalLines, (current * 100.0 / totalLines), line.getTrimmedContent());
                    }
                });
            }

            log.info("✅ Lecture terminée: {} lignes lues avec succès", processedLines.get());
            
            // Parser et regrouper les lignes par msg-type
            log.info("🔧 Début du parsing et regroupement par msg-type...");
            ParsingResult parsingResult = feedParsingService.parseAndGroupLines(allLines);
            
            log.info("📋 Parsing terminé: {} groupes créés avec {} lignes valides", 
                    parsingResult.getGroupCount(), parsingResult.getValidLinesProcessed());
            
            // Anonymiser les msg-types inconnus
            log.info("🔒 Début de l'anonymisation des msg-types inconnus...");
            AnonymizationResult anonymizationResult = anonymizationService.processGroups(
                    parsingResult.getGroupsByMsgType());
            
            log.info("🔒 Anonymisation terminée: {} lignes anonymisées sur {}", 
                    anonymizationResult.getAnonymizedLinesCount(), anonymizationResult.getTotalLinesProcessed());
            
            // Ici, dans les prochaines étapes, nous enverrons les lignes anonymisées au LLM
            if (anonymizationResult.hasUnknownMsgTypes()) {
                log.info("🤖 Début de l'analyse LLM pour {} msg-types inconnus", 
                        anonymizationResult.getUnknownMsgTypesCount());
                
                // Analyser uniquement les msg-types inconnus
                Map<String, List<com.example.chatbotnasoft.dto.AnonymizedLine>> unknownLines = 
                        anonymizationResult.getResultsByMsgType().entrySet().stream()
                                .filter(entry -> anonymizationResult.getUnknownMsgTypes().contains(entry.getKey()))
                                .collect(java.util.stream.Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue
                                ));
                
                LLMAnalysisResult llmResult = llmService.analyzeAnonymizedLines(
                        unknownLines, filePath.getFileName().toString());
                
                log.info("🧠 Analyse LLM terminée: {} lignes analysées avec {:.1f}% de succès", 
                        llmResult.getTotalLinesAnalyzed(), llmResult.getSuccessRate());
                
                // Stocker les mappings dans MongoDB avec déduplication
                if (llmResult.hasSuccessfulMappings()) {
                    log.info("💾 Stockage des mappings LLM dans MongoDB...");
                    feedMappingService.storeMappings(llmResult.getMappings());
                    
                    log.info("✅ Mappings stockés avec succès. Total mappings dans la base: {}", 
                            feedMappingService.getTotalMappingsCount());
                } else {
                    log.info("ℹ️ Aucun mapping valide à stocker");
                }
            } else {
                log.info("✅ Tous les msg-types sont connus - pas d'analyse LLM requise");
            }
            
            // Marquer le fichier comme traité
            fileProcessingService.markFileAsProcessed(filePath);
            log.info("🗂️ Fichier marqué comme traité: {}", filePath.getFileName());
            
        } catch (IOException e) {
            log.error("Erreur lors du traitement du fichier: {}", filePath, e);
        }
    }

    private void processLine(RawFeedLine line) {
        // Pour l'instant, on se contente de logger la ligne
        // Dans les prochaines étapes, nous ajouterons le parsing et le traitement
        log.info("🔧 Traitement de la ligne {} du fichier {}: '{}'", 
                line.getLineNumber(), line.getSourceFileName(), line.getTrimmedContent());
    }

    public void processReadyFiles() {
        log.info("🔍 Vérification des fichiers prêts à traiter...");
        var readyFiles = fileProcessingService.getReadyFiles();
        
        if (readyFiles.isEmpty()) {
            log.info("ℹ️ Aucun fichier prêt à traiter");
            return;
        }

        log.info("📋 Début du traitement de {} fichier(s) prêt(s)", readyFiles.size());
        readyFiles.keySet().forEach(filePath -> {
            log.info("🎯 Traitement du fichier: {}", filePath.getFileName());
            processFeedFile(filePath);
        });
    }
}
