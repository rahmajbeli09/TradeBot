package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.RawFeedLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class FileProcessingService {

    // Map pour suivre les fichiers prêts à être traités
    private final ConcurrentMap<Path, LocalDateTime> readyFiles = new ConcurrentHashMap<>();

    public void markFileAsReady(Path filePath) {
        readyFiles.put(filePath, LocalDateTime.now());
        log.info("Fichier marqué comme prêt pour traitement: {} (à {})", 
                filePath.getFileName(), LocalDateTime.now());
        
        // Ici, dans les prochaines étapes, vous déclencherez le traitement du fichier
        // Pour l'instant, on se contente de le marquer comme prêt
        log.info("📋 Fichier en attente de traitement: {}", filePath.getFileName());
    }

    public boolean isFileReady(Path filePath) {
        return readyFiles.containsKey(filePath);
    }

    public LocalDateTime getReadyTime(Path filePath) {
        return readyFiles.get(filePath);
    }

    public ConcurrentMap<Path, LocalDateTime> getReadyFiles() {
        return new ConcurrentHashMap<>(readyFiles);
    }

    public void markFileAsProcessed(Path filePath) {
        LocalDateTime readyTime = readyFiles.remove(filePath);
        if (readyTime != null) {
            log.info("Fichier marqué comme traité: {} (temps d'attente: {} ms)", 
                    filePath.getFileName(), 
                    java.time.Duration.between(readyTime, LocalDateTime.now()).toMillis());
        }
    }

    public int getReadyFilesCount() {
        return readyFiles.size();
    }

    public void clearReadyFiles() {
        int count = readyFiles.size();
        readyFiles.clear();
        log.info("Nettoyage de {} fichiers prêts", count);
    }
}
