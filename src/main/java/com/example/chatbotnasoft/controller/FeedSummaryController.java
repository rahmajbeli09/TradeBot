package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.ReadableFeedLine;
import com.example.chatbotnasoft.service.FeedSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Controller pour générer des résumés lisibles des fichiers FEED
 */
@RestController
@RequestMapping("/api/feed-summary")
@RequiredArgsConstructor
@Slf4j
public class FeedSummaryController {
    
    private final FeedSummaryService feedSummaryService;
    
    /**
     * Génère un résumé lisible pour un fichier FEED spécifique
     * @param filePath Chemin du fichier FEED à analyser
     * @return Liste des lignes lisibles avec leurs significations
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateReadableSummary(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le paramètre 'filePath' est requis");
            }
            
            // Vérifier si le fichier existe
            File file = new File(filePath);
            if (!file.exists()) {
                return ResponseEntity.badRequest().body("Le fichier n'existe pas: " + filePath);
            }
            
            if (!file.canRead()) {
                return ResponseEntity.badRequest().body("Le fichier n'est pas lisible: " + filePath);
            }
            
            log.info("📄 Génération du résumé lisible pour le fichier: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedSummaryService.generateReadableSummary(filePath);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "filePath", filePath,
                "totalLines", readableLines.size(),
                "readableLines", readableLines
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Génère un résumé lisible pour un fichier FEED dans le répertoire input/feeds
     * @param fileName Nom du fichier (ex: "FEED_20250205.txt")
     * @return Liste des lignes lisibles avec leurs significations
     */
    @PostMapping("/generate-from-input")
    public ResponseEntity<?> generateSummaryFromInputDirectory(@RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le paramètre 'fileName' est requis");
            }
            
            String filePath = "input/feeds/" + fileName;
            
            log.info("📄 Génération du résumé lisible pour le fichier: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedSummaryService.generateReadableSummary(filePath);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "fileName", fileName,
                "filePath", filePath,
                "totalLines", readableLines.size(),
                "readableLines", readableLines
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Génère un résumé textuel formaté
     * @param filePath Chemin du fichier FEED à analyser
     * @return Résumé textuel formaté
     */
    @PostMapping("/generate-text")
    public ResponseEntity<?> generateTextSummary(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le paramètre 'filePath' est requis");
            }
            
            log.info("📄 Génération du résumé textuel pour le fichier: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedSummaryService.generateReadableSummary(filePath);
            String textSummary = feedSummaryService.generateTextSummary(readableLines);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "filePath", filePath,
                "totalLines", readableLines.size(),
                "textSummary", textSummary
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé textuel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint GET pour tester facilement dans le navigateur
     * @param fileName Nom du fichier à analyser
     * @return Résumé lisible du fichier
     */
    @GetMapping("/generate-from-input-browser")
    public ResponseEntity<?> generateSummaryFromInputBrowser(@RequestParam String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le paramètre 'fileName' est requis");
            }
            
            String filePath = "input/feeds/" + fileName;
            
            log.info("📄 Génération du résumé lisible (GET) pour le fichier: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedSummaryService.generateReadableSummary(filePath);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "fileName", fileName,
                "filePath", filePath,
                "totalLines", readableLines.size(),
                "readableLines", readableLines
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Liste les fichiers FEED disponibles dans le répertoire input/feeds
     * @return Liste des fichiers disponibles
     */
    @GetMapping("/available-files")
    public ResponseEntity<?> listAvailableFiles() {
        try {
            File inputDir = new File("input/feeds");
            
            if (!inputDir.exists() || !inputDir.isDirectory()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Le répertoire input/feeds n'existe pas",
                    "files", List.of()
                ));
            }
            
            File[] files = inputDir.listFiles((dir, name) -> name.startsWith("FEED") && name.endsWith(".txt"));
            
            List<String> fileNames = new java.util.ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    fileNames.add(file.getName());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "directory", "input/feeds",
                "totalFiles", fileNames.size(),
                "files", fileNames
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la liste des fichiers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
