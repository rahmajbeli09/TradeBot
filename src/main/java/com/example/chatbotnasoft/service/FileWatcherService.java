package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.FileWatcherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileWatcherService {

    private final FileWatcherProperties properties;
    private final FileStabilizationService stabilizationService;
    private WatchService watchService;
    private ExecutorService executorService;
    private Path watchDirectory;
    private Pattern filePattern;

    @PostConstruct
    public void initializeWatcher() {
        try {
            createInputDirectory();
            setupWatchService();
            startWatching();
            log.info("Surveillance de fichiers démarrée dans: {}", properties.getInputDirectory());
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation de la surveillance", e);
            throw new RuntimeException("Impossible de démarrer la surveillance de fichiers", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (watchService != null) {
                watchService.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            log.info("Surveillance de fichiers arrêtée");
        } catch (IOException e) {
            log.error("Erreur lors de l'arrêt de la surveillance", e);
        }
    }

    private void createInputDirectory() throws IOException {
        watchDirectory = Paths.get(properties.getInputDirectory());
        if (!Files.exists(watchDirectory)) {
            Files.createDirectories(watchDirectory);
            log.info("Répertoire de surveillance créé: {}", watchDirectory);
        }
        filePattern = Pattern.compile(properties.getFilePattern().replace("*", ".*"));
    }

    private void setupWatchService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        watchDirectory.register(watchService, 
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY);
    }

    private void startWatching() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::watchLoop);
    }

    private void watchLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    handleWatchEvent(event);
                }
                
                if (!key.reset()) {
                    log.warn("La clé de surveillance n'est plus valide, arrêt de la surveillance");
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("Surveillance interrompue");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Erreur dans la boucle de surveillance", e);
        }
    }

    private void handleWatchEvent(WatchEvent<?> event) {
        Path fileName = (Path) event.context();
        Path fullPath = watchDirectory.resolve(fileName);
        
        if (!filePattern.matcher(fileName.toString()).matches()) {
            log.debug("Fichier ignoré (pattern non correspondant): {}", fileName);
            return;
        }

        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            log.info("Nouveau fichier détecté: {}", fileName);
            stabilizationService.startStabilization(fullPath);
        } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            log.debug("Fichier modifié: {}", fileName);
            stabilizationService.handleFileModification(fullPath);
        }
    }

    public boolean isWatching() {
        return watchService != null && executorService != null && !executorService.isShutdown();
    }
}
