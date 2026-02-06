package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.ResumeResponse;
import com.example.chatbotnasoft.service.ResumeFeedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller pour générer des résumés structurés des fichiers FEED
 */
@RestController
@RequestMapping("/api/resume-feed")
@Slf4j
public class ResumeFeedController {

    @Autowired
    private ResumeFeedService resumeFeedService;

    /**
     * Génère un résumé complet structuré d'un fichier FEED
     * @param request Body contenant le chemin du fichier
     * @return ResumeResponse avec les données structurées et le résumé textuel
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateResume(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("filePath est requis"));
            }

            log.info("📊 Génération de résumé demandée pour: {}", filePath);
            
            ResumeResponse resumeResponse = resumeFeedService.generateResume(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("resumeData", resumeResponse.getResumeData());
            response.put("resumeTexte", resumeResponse.getResumeTexte());
            response.put("statistiques", resumeResponse.getStatistiques());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Génère un résumé pour un fichier FEED spécifique (GET pour test navigateur)
     * @param fileName Nom du fichier FEED (doit être dans input/feeds/)
     * @return ResumeResponse avec les données structurées
     */
    @GetMapping("/generate/{fileName}")
    public ResponseEntity<Map<String, Object>> generateResumeGet(@PathVariable String fileName) {
        try {
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
            
            String filePath = "input/feeds/" + fileName;
            log.info("📊 Génération de résumé (GET) pour: {}", filePath);
            
            ResumeResponse resumeResponse = resumeFeedService.generateResume(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("fileName", fileName);
            response.put("resumeData", resumeResponse.getResumeData());
            response.put("resumeTexte", resumeResponse.getResumeTexte());
            response.put("statistiques", resumeResponse.getStatistiques());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé (GET): {}", e.getMessage(), e);
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
            List<String> feedFiles = resumeFeedService.listAvailableFeedFiles();
            
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
     * Endpoint de test pour vérifier que le service fonctionne
     * @return Message de statut du service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "ResumeFeedService");
        response.put("status", "Opérationnel");
        response.put("description", "Service de génération de résumés structurés pour fichiers FEED");
        
        try {
            List<String> availableFiles = resumeFeedService.listAvailableFeedFiles();
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
