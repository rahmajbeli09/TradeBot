package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.config.FileWatcherProperties;
import com.example.chatbotnasoft.service.FileProcessingService;
import com.example.chatbotnasoft.service.FileStabilizationService;
import com.example.chatbotnasoft.service.FileWatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file-watcher")
@RequiredArgsConstructor
@Slf4j
public class FileWatcherController {

    private final FileWatcherService fileWatcherService;
    private final FileStabilizationService stabilizationService;
    private final FileProcessingService processingService;
    private final FileWatcherProperties properties;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("isWatching", fileWatcherService.isWatching());
        status.put("inputDirectory", properties.getInputDirectory());
        status.put("filePattern", properties.getFilePattern());
        status.put("stabilizationDelayMinutes", properties.getStabilizationDelayMinutes());
        status.put("checkIntervalSeconds", properties.getCheckIntervalSeconds());
        status.put("timestamp", LocalDateTime.now());
        
        // État des fichiers en stabilisation
        Map<String, Object> stabilizingFiles = new HashMap<>();
        stabilizationService.getStabilizingFiles().forEach((path, info) -> {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("startTime", info.getStartTime());
            fileInfo.put("lastModificationTime", info.getLastModificationTime());
            fileInfo.put("lastKnownSize", info.getLastKnownSize());
            stabilizingFiles.put(path.getFileName().toString(), fileInfo);
        });
        status.put("stabilizingFiles", stabilizingFiles);
        
        // Fichiers prêts pour traitement
        Map<String, Object> readyFiles = new HashMap<>();
        processingService.getReadyFiles().forEach((path, readyTime) -> {
            readyFiles.put(path.getFileName().toString(), readyTime);
        });
        status.put("readyFiles", readyFiles);
        status.put("readyFilesCount", processingService.getReadyFilesCount());
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/ready-files")
    public ResponseEntity<Map<String, Object>> getReadyFiles() {
        Map<String, Object> response = new HashMap<>();
        response.put("readyFiles", processingService.getReadyFiles());
        response.put("count", processingService.getReadyFilesCount());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear-ready-files")
    public ResponseEntity<Map<String, Object>> clearReadyFiles() {
        int count = processingService.getReadyFilesCount();
        processingService.clearReadyFiles();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fichiers prêts nettoyés");
        response.put("clearedCount", count);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stabilizing-files")
    public ResponseEntity<Map<String, Object>> getStabilizingFiles() {
        Map<String, Object> response = new HashMap<>();
        response.put("stabilizingFiles", stabilizationService.getStabilizingFiles());
        response.put("count", stabilizationService.getStabilizingFiles().size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
