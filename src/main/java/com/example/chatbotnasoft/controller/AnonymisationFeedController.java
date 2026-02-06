package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.AnonymisationResponse;
import com.example.chatbotnasoft.service.AnonymisationFeedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller pour anonymiser les lignes de fichiers FEED
 * Prépare les données pour l'envoi à Gemini LLM
 */
@RestController
@RequestMapping("/api/anonymiser-feed")
@Slf4j
public class AnonymisationFeedController {

    @Autowired
    private AnonymisationFeedService anonymisationFeedService;

    /**
     * Anonymise un fichier FEED complet selon les règles spécifiées
     * @param request Body contenant le chemin du fichier
     * @return AnonymisationResponse avec les lignes anonymisées prêtes pour Gemini
     */
    @PostMapping("/anonymiser")
    public ResponseEntity<Map<String, Object>> anonymiserFichier(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("filePath est requis"));
            }

            log.info("🔒 Anonymisation demandée pour: {}", filePath);
            
            AnonymisationResponse response = anonymisationFeedService.anonymiserFichier(filePath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", response.getFileName());
            result.put("lignesAnonymisees", response.getLignesAnonymisees());
            result.put("contenuAnonymise", response.getContenuAnonymise());
            result.put("statistiques", response.getStatistiques());
            result.put("success", response.isSuccess());
            
            if (!response.isSuccess()) {
                result.put("erreur", response.getErreur());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'anonymisation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Anonymise un fichier FEED spécifique (GET pour test navigateur)
     * @param fileName Nom du fichier FEED (doit être dans input/feeds/)
     * @return AnonymisationResponse avec les lignes anonymisées
     */
    @GetMapping("/anonymiser/{fileName}")
    public ResponseEntity<Map<String, Object>> anonymiserFichierGet(@PathVariable String fileName) {
        try {
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
            
            String filePath = "input/feeds/" + fileName;
            log.info("🔒 Anonymisation (GET) pour: {}", filePath);
            
            AnonymisationResponse response = anonymisationFeedService.anonymiserFichier(filePath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", response.getFileName());
            result.put("lignesAnonymisees", response.getLignesAnonymisees());
            result.put("contenuAnonymise", response.getContenuAnonymise());
            result.put("statistiques", response.getStatistiques());
            result.put("success", response.isSuccess());
            
            if (!response.isSuccess()) {
                result.put("erreur", response.getErreur());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'anonymisation (GET): {}", e.getMessage(), e);
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
            List<String> feedFiles = anonymisationFeedService.listAvailableFeedFiles();
            
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
     * Endpoint de test pour vérifier que le service d'anonymisation fonctionne
     * @return Message de statut du service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "AnonymisationFeedService");
        response.put("status", "Opérationnel");
        response.put("description", "Service d'anonymisation de fichiers FEED pour préparation à Gemini LLM");
        response.put("version", "1.0");
        
        // Règles d'anonymisation appliquées
        Map<String, String> regles = new HashMap<>();
        regles.put("champs_1_2_3", "Conservés sans modification");
        regles.put("champ_4_plus", "Anonymisés selon type détecté");
        regles.put("date", "Format YYYYMMDD conservé");
        regles.put("heure", "Format HHMMSS conservé");
        regles.put("identifiant", "Transformé en ID_XXXXX");
        regles.put("nombre", "Transformé en NUM_XXXX...");
        regles.put("code", "Transformé en CODE_XX");
        regles.put("vide", "Laissé vide");
        response.put("reglesAnonymisation", regles);
        
        try {
            List<String> availableFiles = anonymisationFeedService.listAvailableFeedFiles();
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
