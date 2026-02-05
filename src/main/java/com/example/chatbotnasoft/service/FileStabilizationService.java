package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.FileWatcherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStabilizationService {

    private final FileWatcherProperties properties;
    private final FileProcessingService processingService;
    private final FeedProcessingService feedProcessingService;
    
    // Map pour suivre l'état des fichiers en cours de stabilisation
    private final Map<Path, FileStabilizationInfo> stabilizingFiles = new ConcurrentHashMap<>();

    @Async
    public void startStabilization(Path filePath) {
        if (stabilizingFiles.containsKey(filePath)) {
            log.debug("Fichier déjà en cours de stabilisation: {}", filePath);
            return;
        }

        FileStabilizationInfo info = new FileStabilizationInfo(filePath);
        stabilizingFiles.put(filePath, info);
        
        log.info("Début de stabilisation pour: {} (délai: {} minutes)", 
                filePath.getFileName(), properties.getStabilizationDelayMinutes());
    }

    public void handleFileModification(Path filePath) {
        FileStabilizationInfo info = stabilizingFiles.get(filePath);
        if (info != null) {
            info.resetStabilizationTime();
            log.debug("Stabilisation réinitialisée pour: {} (fichier modifié)", filePath.getFileName());
        }
    }

    @Scheduled(fixedDelayString = "#{@fileWatcherProperties.checkIntervalSeconds * 1000}")
    public void checkStabilization() {
        if (stabilizingFiles.isEmpty()) {
            return;
        }

        log.debug("Vérification de stabilisation de {} fichier(s)", stabilizingFiles.size());

        stabilizingFiles.entrySet().removeIf(entry -> {
            Path filePath = entry.getKey();
            FileStabilizationInfo info = entry.getValue();

            try {
                if (!Files.exists(filePath)) {
                    log.warn("Fichier disparu pendant la stabilisation: {}", filePath);
                    return true; // Supprimer de la map
                }

                long currentSize = Files.size(filePath);
                if (currentSize != info.getLastKnownSize()) {
                    info.updateSize(currentSize);
                    log.debug("Taille du fichier modifiée pour: {} ({} -> {} octets)", 
                            filePath.getFileName(), info.getLastKnownSize(), currentSize);
                    return false; // Garder dans la map
                }

                // Vérifier si le délai de stabilisation est écoulé
                if (info.isStabilized(properties.getStabilizationDelayMillis())) {
                    log.info("Fichier stabilisé et prêt pour traitement: {}", filePath.getFileName());
                    processingService.markFileAsReady(filePath);
                    
                    // Déclencher immédiatement le traitement du fichier
                    log.info("🚀 Déclenchement immédiat du traitement pour: {}", filePath.getFileName());
                    feedProcessingService.processFeedFile(filePath);
                    
                    return true; // Supprimer de la map
                }

                return false; // Garder dans la map

            } catch (IOException e) {
                log.error("Erreur lors de la vérification du fichier: {}", filePath, e);
                return true; // Supprimer de la map en cas d'erreur
            }
        });
    }

    public Map<Path, FileStabilizationInfo> getStabilizingFiles() {
        return new ConcurrentHashMap<>(stabilizingFiles);
    }

    public static class FileStabilizationInfo {
        private final Path filePath;
        private final LocalDateTime startTime;
        private LocalDateTime lastModificationTime;
        private long lastKnownSize = -1;

        public FileStabilizationInfo(Path filePath) {
            this.filePath = filePath;
            this.startTime = LocalDateTime.now();
            this.lastModificationTime = startTime;
            try {
                this.lastKnownSize = Files.size(filePath);
            } catch (IOException e) {
                log.warn("Impossible d'obtenir la taille initiale du fichier: {}", filePath);
            }
        }

        public void resetStabilizationTime() {
            this.lastModificationTime = LocalDateTime.now();
        }

        public void updateSize(long newSize) {
            this.lastKnownSize = newSize;
            this.lastModificationTime = LocalDateTime.now();
        }

        public boolean isStabilized(long stabilizationDelayMillis) {
            long elapsedMillis = java.time.Duration.between(lastModificationTime, LocalDateTime.now()).toMillis();
            return elapsedMillis >= stabilizationDelayMillis;
        }

        public Path getFilePath() { return filePath; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getLastModificationTime() { return lastModificationTime; }
        public long getLastKnownSize() { return lastKnownSize; }
    }
}
