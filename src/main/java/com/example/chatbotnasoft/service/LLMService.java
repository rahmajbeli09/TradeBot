package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.GeminiProperties;
import com.example.chatbotnasoft.dto.AnonymizedLine;
import com.example.chatbotnasoft.dto.FieldMapping;
import com.example.chatbotnasoft.dto.LLMAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public LLMAnalysisResult analyzeAnonymizedLines(Map<String, List<AnonymizedLine>> linesByMsgType, 
                                                 String sourceFileName) {
        log.info("🤖 Début de l'analyse LLM pour {} msg-types", linesByMsgType.size());
        
        Map<String, List<FieldMapping>> resultsByMsgType = new HashMap<>();
        List<String> analysisErrors = new ArrayList<>();
        int totalLinesAnalyzed = 0;

        // Analyser chaque msg-type en parallèle
        List<CompletableFuture<Void>> futures = linesByMsgType.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    String msgType = entry.getKey();
                    List<AnonymizedLine> lines = entry.getValue();
                    
                    log.info("🔍 Analyse du msg-type '{}' avec {} lignes", msgType, lines.size());
                    
                    List<FieldMapping> mappings = analyzeLinesForMsgType(msgType, lines, analysisErrors);
                    
                    synchronized (resultsByMsgType) {
                        resultsByMsgType.put(msgType, mappings);
                    }
                    
                    log.info("✅ Analyse terminée pour msg-type '{}': {} mappings créés", 
                            msgType, mappings.size());
                }, executorService))
                .collect(Collectors.toList());

        // Attendre la fin de toutes les analyses
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        totalLinesAnalyzed = linesByMsgType.values().stream()
                .mapToInt(List::size)
                .sum();

        LLMAnalysisResult result = new LLMAnalysisResult(resultsByMsgType, analysisErrors, 
                totalLinesAnalyzed, sourceFileName);
        
        logAnalysisSummary(result);
        
        return result;
    }

    private List<FieldMapping> analyzeLinesForMsgType(String msgType, List<AnonymizedLine> lines, 
                                                   List<String> analysisErrors) {
        List<FieldMapping> mappings = new ArrayList<>();
        
        // Analyser la première ligne pour déterminer la structure
        if (lines.isEmpty()) {
            return mappings;
        }

        AnonymizedLine firstLine = lines.get(0);
        String prompt = buildAnalysisPrompt(firstLine.getAnonymizedLine());
        
        try {
            String response = callGeminiAPI(prompt);
            Map<String, String> fieldMapping = parseGeminiResponse(response);
            
            // Appliquer le même mapping à toutes les lignes du même msg-type
            for (AnonymizedLine line : lines) {
                FieldMapping mapping = new FieldMapping(
                        msgType,
                        new HashMap<>(fieldMapping), // Copie du mapping
                        line.getOriginalLine(),
                        line.getAnonymizedLine(),
                        line.getAnonymizedLine().split(";").length
                );
                
                if (mapping.isValid()) {
                    mappings.add(mapping);
                } else {
                    analysisErrors.add(String.format("Mapping invalide pour msg-type %s, ligne %d", 
                            msgType, line.getLineNumber()));
                }
            }
            
        } catch (Exception e) {
            String error = String.format("Erreur lors de l'analyse du msg-type %s: %s", 
                    msgType, e.getMessage());
            analysisErrors.add(error);
            log.error("❌ {}", error, e);
        }
        
        return mappings;
    }

    private String buildAnalysisPrompt(String anonymizedLine) {
        return String.format("""
                Analyse cette ligne de feed anonymisée :
                Ligne : %s
                
                - Détecte dynamiquement la signification de chaque champ en fonction du msg-type
                - **RÉPONSE OBLIGATOIREMENT EN JSON UNIQUEMENT** - Pas de texte avant ou après
                - Format exact requis :
                {
                  "Champ 1": "Signification",
                  "Champ 2": "Signification", 
                  "Champ 3": "Signification",
                  ...
                }
                - **IMPORTANT** : Ne réponds qu'avec le JSON, sans aucun commentaire ou explication
                - Le nombre de champs doit correspondre exactement au nombre de champs dans la ligne
                """, anonymizedLine);
    }

    private String callGeminiAPI(String prompt) {
        // Utiliser des modèles qui existent vraiment dans l'API v1beta
        String url = String.format("%s/models/%s:generateContent?key=%s", 
                geminiProperties.getBaseUrl(), "gemini-2.5-flash", geminiProperties.getApiKey());
        
        try {
            return makeApiCall(url, prompt);
        } catch (Exception e) {
            log.warn("⚠️ Échec avec gemini-2.5-flash, tentative avec gemini-2.0-flash");
            
            // Fallback vers gemini-2.0-flash si le modèle principal échoue
            String fallbackUrl = String.format("%s/models/%s:generateContent?key=%s", 
                    geminiProperties.getBaseUrl(), "gemini-2.0-flash", geminiProperties.getApiKey());
            return makeApiCall(fallbackUrl, prompt);
        }
    }
    
    private String makeApiCall(String url, String prompt) {

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", geminiProperties.getTemperature(),
                        "maxOutputTokens", geminiProperties.getMaxTokens()
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            } else {
                throw new RuntimeException("Réponse invalide de Gemini: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à Gemini API", e);
            throw new RuntimeException("Échec de l'appel à Gemini: " + e.getMessage(), e);
        }
    }

    private String extractTextFromResponse(Map responseBody) {
        try {
            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map candidate = candidates.get(0);
                Map content = (Map) candidate.get("content");
                List<Map> parts = (List<Map>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            throw new RuntimeException("Format de réponse Gemini invalide");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction du texte de la réponse Gemini", e);
        }
    }

    private Map<String, String> parseGeminiResponse(String response) {
        try {
            log.debug("🔍 Réponse Gemini brute: {}", response);
            
            // Extraire le JSON de la réponse
            String jsonContent = extractJsonFromResponse(response);
            
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            Map<String, String> fieldMapping = new HashMap<>();
            
            jsonNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue().asText();
                fieldMapping.put(fieldName, fieldValue);
            });
            
            log.debug("📋 Mapping extrait: {}", fieldMapping);
            return fieldMapping;
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du parsing de la réponse Gemini: {}", response, e);
            throw new RuntimeException("Échec du parsing JSON: " + e.getMessage(), e);
        }
    }

    private String extractJsonFromResponse(String response) {
        log.debug("🔍 Réponse Gemini brute: {}", response);
        
        // Chercher le JSON dans la réponse (peut être entouré de texte)
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            String jsonStr = response.substring(jsonStart, jsonEnd + 1);
            log.debug("📋 JSON extrait: {}", jsonStr);
            return jsonStr;
        }
        
        // Si pas de JSON complet, essayer de reconstruire depuis le début
        if (jsonStart != -1) {
            String partialJson = response.substring(jsonStart);
            log.debug("📋 JSON partiel trouvé: {}", partialJson);
            
            // Essayer de compléter le JSON s'il est tronqué
            if (partialJson.contains("\"Champ")) {
                // Compter les champs ouverts et fermés
                int openBraces = 0;
                int closeBraces = 0;
                boolean inString = false;
                
                for (int i = 0; i < partialJson.length(); i++) {
                    char c = partialJson.charAt(i);
                    if (c == '"' && (i == 0 || partialJson.charAt(i-1) != '\\')) {
                        inString = !inString;
                    }
                    if (!inString) {
                        if (c == '{') openBraces++;
                        else if (c == '}') closeBraces++;
                    }
                }
                
                // Ajouter les accolades manquantes
                while (closeBraces < openBraces) {
                    partialJson += "}";
                    closeBraces++;
                }
                
                log.debug("📋 JSON reconstruit: {}", partialJson);
                return partialJson;
            }
        }
        
        throw new RuntimeException("Aucun JSON trouvé dans la réponse Gemini");
    }

    private void logAnalysisSummary(LLMAnalysisResult result) {
        log.info("📊 Résumé de l'analyse LLM:");
        log.info("   • Lignes totales analysées: {}", result.getTotalLinesAnalyzed());
        log.info("   • Analyses réussies: {}", result.getSuccessfulAnalyses());
        log.info("   • Analyses échouées: {}", result.getFailedAnalyses());
        log.info("   • Msg-types traités: {}", result.getMsgTypeCount());
        log.info("   • Taux de succès: {:.1f}%", result.getSuccessRate());
        
        if (result.hasErrors()) {
            log.warn("⚠️ Erreurs d'analyse (3 premières sur {}):", result.getAnalysisErrors().size());
            result.getAnalysisErrors().stream().limit(3).forEach(error -> 
                    log.warn("   • {}", error));
        }
        
        // Détail par msg-type
        result.getResultsByMsgType().forEach((msgType, mappings) -> {
            log.info("   • Msg-type '{}': {} mappings", msgType, mappings.size());
        });
    }
}
