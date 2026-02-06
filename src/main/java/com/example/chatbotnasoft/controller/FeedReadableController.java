package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.ReadableFeedLine;
import com.example.chatbotnasoft.service.FeedReadableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller pour générer des résumés lisibles des fichiers FEED
 */
@RestController
@RequestMapping("/api/feed-readable")
@Slf4j
public class FeedReadableController {

    @Autowired
    private FeedReadableService feedReadableService;

    /**
     * Génère une représentation lisible d'un fichier FEED spécifique
     * @param request Body contenant le chemin du fichier
     * @return Liste des lignes lisibles avec leurs significations
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReadableFeed(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("filePath est requis"));
            }

            log.info("📖 Génération lisible demandée pour: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedReadableService.generateReadableFeed(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("totalLines", readableLines.size());
            response.put("linesWithMapping", (int) readableLines.stream().filter(ReadableFeedLine::hasMapping).count());
            response.put("linesWithoutMapping", (int) readableLines.stream().filter(line -> !line.hasMapping()).count());
            response.put("readableLines", readableLines);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération lisible: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Liste les fichiers FEED disponibles
     * @return Liste des noms de fichiers FEED disponibles
     */
    @GetMapping("/list-files")
    public ResponseEntity<Map<String, Object>> listFeedFiles() {
        try {
            List<String> feedFiles = feedReadableService.listAvailableFeedFiles();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalFiles", feedFiles.size());
            response.put("files", feedFiles);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la liste des fichiers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Génère un résumé textuel d'un fichier FEED
     * @param request Body contenant le chemin du fichier
     * @return Résumé textuel formaté
     */
    @PostMapping("/generate-text-summary")
    public ResponseEntity<Map<String, Object>> generateTextSummary(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("filePath est requis"));
            }

            log.info("📝 Génération résumé textuel demandée pour: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedReadableService.generateReadableFeed(filePath);
            String textSummary = feedReadableService.generateTextSummary(readableLines);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("totalLines", readableLines.size());
            response.put("linesWithMapping", (int) readableLines.stream().filter(ReadableFeedLine::hasMapping).count());
            response.put("linesWithoutMapping", (int) readableLines.stream().filter(line -> !line.hasMapping()).count());
            response.put("textSummary", textSummary);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé textuel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Génère une représentation lisible pour un fichier FEED spécifique (GET pour test navigateur)
     * @param fileName Nom du fichier FEED (doit être dans input/feeds/)
     * @return Liste des lignes lisibles
     */
    @GetMapping("/generate/{fileName}")
    public ResponseEntity<Map<String, Object>> generateReadableFeedGet(@PathVariable String fileName) {
        try {
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
            
            String filePath = "input/feeds/" + fileName;
            log.info("📖 Génération lisible (GET) pour: {}", filePath);
            
            List<ReadableFeedLine> readableLines = feedReadableService.generateReadableFeed(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("fileName", fileName);
            response.put("totalLines", readableLines.size());
            response.put("linesWithMapping", (int) readableLines.stream().filter(ReadableFeedLine::hasMapping).count());
            response.put("linesWithoutMapping", (int) readableLines.stream().filter(line -> !line.hasMapping()).count());
            response.put("readableLines", readableLines);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération lisible (GET): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de test pour vérifier que le service fonctionne
     * @return Message de statut du service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "FeedReadableService");
        response.put("status", "Opérationnel");
        response.put("description", "Service de génération de résumés lisibles pour fichiers FEED");
        
        try {
            List<String> availableFiles = feedReadableService.listAvailableFeedFiles();
            response.put("availableFilesCount", availableFiles.size());
            response.put("availableFiles", availableFiles);
        } catch (Exception e) {
            response.put("availableFilesCount", 0);
            response.put("availableFilesError", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Crée une réponse d'erreur standardisée
     * @param message Message d'erreur
     * @return Map d'erreur
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
