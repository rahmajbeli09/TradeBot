package com.example.chatbotnasoft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomaticProcessingService {

    private final FeedProcessingService feedProcessingService;
    private final FileProcessingService fileProcessingService;

    @Scheduled(fixedDelayString = "#{@fileWatcherProperties.checkIntervalSeconds * 1000}")
    public void processReadyFilesAutomatically() {
        int readyFilesCount = fileProcessingService.getReadyFilesCount();
        
        if (readyFilesCount > 0) {
            log.info("🤖 Déclenchement automatique du traitement pour {} fichier(s) prêt(s)", readyFilesCount);
            feedProcessingService.processReadyFiles();
        }
    }
}
