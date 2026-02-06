package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.ResumeResponseAmeliore;
import com.example.chatbotnasoft.service.ResumeFeedAmelioreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller amélioré pour générer des résumés structurés complets des fichiers FEED
 */
@RestController
@RequestMapping("/api/resume-feed-ameliore")
@Slf4j
public class ResumeFeedAmelioreController {

    @Autowired
    private ResumeFeedAmelioreService resumeFeedAmelioreService;

    /**
     * Génère un résumé complet et amélioré d'un fichier FEED
     * @param request Body contenant le chemin du fichier
     * @return ResumeResponseAmeliore avec les données structurées et le résumé textuel amélioré
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateResumeAmeliore(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("filePath est requis"));
            }

            log.info("📊 Génération de résumé amélioré demandée pour: {}", filePath);
            
            ResumeResponseAmeliore resumeResponse = resumeFeedAmelioreService.generateResumeAmeliore(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", resumeResponse.getFileName());
            response.put("resumeDataAmeliore", resumeResponse.getResumeDataAmeliore());
            response.put("resumeTexteAmeliore", resumeResponse.getResumeTexteAmeliore());
            response.put("statistiques", resumeResponse.getStatistiques());
            response.put("success", resumeResponse.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé amélioré: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Génère un résumé amélioré pour un fichier FEED spécifique (GET pour test navigateur)
     * @param fileName Nom du fichier FEED (doit être dans input/feeds/)
     * @return ResumeResponseAmeliore avec les données structurées
     */
    @GetMapping("/generate/{fileName}")
    public ResponseEntity<Map<String, Object>> generateResumeAmelioreGet(@PathVariable String fileName) {
        try {
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
            
            String filePath = "input/feeds/" + fileName;
            log.info("📊 Génération de résumé amélioré (GET) pour: {}", filePath);
            
            ResumeResponseAmeliore resumeResponse = resumeFeedAmelioreService.generateResumeAmeliore(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", resumeResponse.getFileName());
            response.put("resumeDataAmeliore", resumeResponse.getResumeDataAmeliore());
            response.put("resumeTexteAmeliore", resumeResponse.getResumeTexteAmeliore());
            response.put("statistiques", resumeResponse.getStatistiques());
            response.put("success", resumeResponse.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé amélioré (GET): {}", e.getMessage(), e);
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
            List<String> feedFiles = resumeFeedAmelioreService.listAvailableFeedFiles();
            
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
     * Endpoint de test pour vérifier que le service amélioré fonctionne
     * @return Message de statut du service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "ResumeFeedAmelioreService");
        response.put("status", "Opérationnel");
        response.put("description", "Service amélioré de génération de résumés structurés pour fichiers FEED");
        response.put("version", "2.0 - Amélioré");
        
        try {
            List<String> availableFiles = resumeFeedAmelioreService.listAvailableFeedFiles();
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
