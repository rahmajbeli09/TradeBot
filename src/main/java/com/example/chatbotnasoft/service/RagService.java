package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final GeminiEmbeddingService geminiEmbeddingService;
    private final QdrantClient qdrantClient;
    private final SimpleLlmService simpleLlmService;
    private final FeedMappingRepository feedMappingRepository;
    private final ObjectMapper objectMapper;

    public RagResponse ask(String question, int limit) {
        long startTime = System.currentTimeMillis();

        // 1. Embedding de la question
        long embeddingStart = System.currentTimeMillis();
        List<Double> queryVector = geminiEmbeddingService.embed(question);
        long embeddingTime = System.currentTimeMillis() - embeddingStart;

        if (queryVector == null || queryVector.isEmpty()) {
            return RagResponse.error("Impossible de générer l'embedding de la question");
        }

        // 2. Recherche dans Qdrant
        long searchStart = System.currentTimeMillis();
        String searchResult = qdrantClient.search(queryVector, limit);
        long searchTime = System.currentTimeMillis() - searchStart;

        if (searchResult == null || searchResult.isBlank()) {
            return RagResponse.error("Aucun document pertinent trouvé");
        }

        // 3. Extraction des mappings
        List<RagContext> contexts = extractContexts(searchResult);
        if (contexts.isEmpty()) {
            logError("MESSAGE_INCONNU", "Aucun mapping trouvé dans Qdrant", Map.of("question", question));
            return RagResponse.error("Désolé, je n'ai pas d'information sur ce message.");
        }

        // 4. Validation du msgType principal
        RagValidation validation = validateMsgType(question, contexts);
        if (!validation.valid()) {
            logError("VALIDATION_MSGTYPE", validation.getErrorMessage(), Map.of("question", question, "contexts", contexts));
            return RagResponse.error(validation.getErrorMessage());
        }

        // 5. Validation du seuil de confiance
        RagContext topContext = validation.getPrimaryContext();
        if (topContext.score() < 0.65) {
            logError("SEUIL_CONFIANCE", "Score trop bas", Map.of("score", topContext.score(), "question", question));
            return RagResponse.error("Désolé, la question est trop ambiguë pour fournir une réponse fiable.");
        }

        // 6. Construction du contexte validé
        String contextText = buildValidatedContext(validation.getPrimaryContext(), validation.getSecondaryContexts());

        // 7. Génération de la réponse
        long llmStart = System.currentTimeMillis();
        String answer = simpleLlmService.generate(question, contextText);
        long llmTime = System.currentTimeMillis() - llmStart;

        if (answer == null || answer.isBlank()) {
            logError("REPONSE_VIDE", "LLM a retourné une réponse vide", Map.of("question", question));
            return RagResponse.error("Désolé, je n'ai pas pu générer de réponse fiable.");
        }

        // 8. Validation de cohérence de la réponse
        if (!isResponseCoherente(answer, topContext.msgType(), question)) {
            logError("REPONSE_INCOHERENTE", "La réponse ne correspond pas au msgType principal", 
                     Map.of("answer", answer, "expectedMsgType", topContext.msgType(), "question", question));
            return RagResponse.error("Désolé, la question est ambiguë ou la réponse n'est pas fiable.");
        }

        long finalTotalTime = System.currentTimeMillis() - startTime;

        // 6. Validation des performances
        RagPerformanceValidation perfValidation = validatePerformance(embeddingTime, searchTime, llmTime, finalTotalTime);
        if (!perfValidation.valid()) {
            log.warn("⚠️ Performance alert: {}", perfValidation.getWarnings());
        }

        return RagResponse.success(
                question,
                answer.trim(),
                validation.getAllContexts(),
                RagMetadata.of(embeddingTime, searchTime, llmTime, finalTotalTime, perfValidation)
        );
    }

    private RagPerformanceValidation validatePerformance(long embeddingTime, long searchTime, long llmTime, long totalTime) {
        List<String> warnings = new ArrayList<>();
        
        // Seuils indicatifs
        if (embeddingTime > 1500) {
            warnings.add(String.format("Embedding lent: %dms (seuil: 1500ms)", embeddingTime));
        }
        if (searchTime > 100) {
            warnings.add(String.format("Recherche lente: %dms (seuil: 100ms)", searchTime));
        }
        if (llmTime > 50) {
            warnings.add(String.format("LLM lent: %dms (seuil: 50ms)", llmTime));
        }
        if (totalTime > 2000) {
            warnings.add(String.format("Temps total élevé: %dms (seuil: 2000ms)", totalTime));
        }
        
        return new RagPerformanceValidation(warnings.isEmpty(), warnings);
    }

    private void logError(String errorType, String message, Map<String, Object> payload) {
        log.error("❌ ERREUR RAG [{}] : {} | Payload: {}", errorType, message, payload);
    }

    private boolean isResponseCoherente(String answer, String expectedMsgType, String question) {
        // Vérifier que la réponse mentionne bien le msgType attendu
        if (question.toLowerCase().contains("msgtype")) {
            String questionMsgType = extractMsgTypeFromQuestion(question);
            if (questionMsgType != null && !answer.toLowerCase().contains(questionMsgType.toLowerCase())) {
                log.warn("⚠️ Incohérence: msgType question '{}' non trouvé dans réponse", questionMsgType);
                return false;
            }
        }
        
        // Vérifier que la réponse ne contient pas d'informations contradictoires
        if (answer.toLowerCase().contains("désolé") && answer.length() < 50) {
            log.warn("⚠️ Incohérence: réponse de refus mais contexte valide");
            return false;
        }
        
        return true;
    }

    private List<RagContext> extractContexts(String searchResult) {
        List<RagContext> contexts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(searchResult);
            JsonNode points = root.path("result");
            for (JsonNode point : points) {
                JsonNode payload = point.path("payload");
                String msgType = payload.path("msgType").asText();
                if (msgType.isBlank()) continue;

                // Vérifier que le document est Validé
                String status = payload.path("status").asText();
                if (!"Validé".equals(status)) continue;

                FeedMapping mapping = feedMappingRepository.findByMsgTypeAndIsActive(msgType, true);
                if (mapping != null) {
                    double score = point.path("score").asDouble();
                    contexts.add(new RagContext(
                            point.path("id").asText(),
                            msgType,
                            score,
                            mapping.getMapping()
                    ));
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur extraction contexts: {}", e.getMessage());
        }
        return contexts;
    }

    private RagValidation validateMsgType(String question, List<RagContext> contexts) {
        if (contexts.isEmpty()) {
            return RagValidation.invalid("Aucun contexte disponible pour validation");
        }

        // Extraire le msgType mentionné dans la question
        String mentionedMsgType = extractMsgTypeFromQuestion(question);
        
        // Si msgType mentionné, forcer son utilisation même si score inférieur
        if (mentionedMsgType != null) {
            RagContext mentionedContext = findContextByMsgType(contexts, mentionedMsgType);
            if (mentionedContext != null) {
                List<RagContext> secondaryContexts = contexts.stream()
                        .filter(ctx -> !ctx.msgType().equals(mentionedMsgType))
                        .limit(2)
                        .toList();
                return RagValidation.valid(mentionedContext, secondaryContexts);
            } else {
                return RagValidation.invalid("Le msgType " + mentionedMsgType + " mentionné n'existe pas dans les mappings validés");
            }
        }

        // Sinon, utiliser uniquement le top 1 vectoriel
        RagContext topContext = contexts.get(0); // Déjà trié par score décroissant
        List<RagContext> secondaryContexts = contexts.stream()
                .skip(1)
                .limit(2)
                .toList();
        
        return RagValidation.valid(topContext, secondaryContexts);
    }

    private String extractMsgTypeFromQuestion(String question) {
        // Pattern pour trouver "msgType X" dans la question
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("msgType\\s+(\\w+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(question);
        return matcher.find() ? matcher.group(1) : null;
    }

    private RagContext findContextByMsgType(List<RagContext> contexts, String msgType) {
        return contexts.stream()
                .filter(ctx -> ctx.msgType().equalsIgnoreCase(msgType))
                .findFirst()
                .orElse(null);
    }

    private String buildValidatedContext(RagContext primaryContext, List<RagContext> secondaryContexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contexte principal (msgType ").append(primaryContext.msgType()).append(") :\n");
        primaryContext.mapping().forEach((champ, signification) ->
                sb.append(String.format("- %s : %s\n", champ, signification))
        );
        
        if (!secondaryContexts.isEmpty()) {
            sb.append("\nContextes secondaires :\n");
            for (RagContext ctx : secondaryContexts) {
                sb.append(String.format("MsgType %s (score: %.2f)\n", ctx.msgType(), ctx.score()));
            }
        }
        
        return sb.toString();
    }

    private String buildPrompt(String question, String context) {
        return String.format(
                "En te basant sur le contexte ci-dessous, réponds à la question utilisateur de manière précise et concise :\n\n" +
                "Question : %s\n\n" +
                "Contexte :\n%s\n\n" +
                "Réponse attendue : une seule phrase claire basée sur les informations du contexte.",
                question, context
        );
    }

    public record RagValidation(
            boolean valid,
            RagContext primaryContext,
            List<RagContext> secondaryContexts,
            String errorMessage
    ) {
        public static RagValidation valid(RagContext primaryContext, List<RagContext> secondaryContexts) {
            return new RagValidation(true, primaryContext, secondaryContexts, null);
        }

        public static RagValidation invalid(String errorMessage) {
            return new RagValidation(false, null, null, errorMessage);
        }

        public List<RagContext> getAllContexts() {
            if (primaryContext == null) return secondaryContexts != null ? secondaryContexts : List.of();
            
            List<RagContext> all = new ArrayList<>();
            all.add(primaryContext);
            if (secondaryContexts != null) {
                all.addAll(secondaryContexts);
            }
            return all;
        }
        
        public RagContext getPrimaryContext() {
            return primaryContext;
        }
        
        public List<RagContext> getSecondaryContexts() {
            return secondaryContexts != null ? secondaryContexts : List.of();
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public record RagPerformanceValidation(
            boolean valid,
            List<String> warnings
    ) {
        public List<String> getWarnings() {
            return warnings != null ? warnings : List.of();
        }
        
        public boolean valid() {
            return valid;
        }
    }

    public record RagMetadata(
            long embeddingTimeMs,
            long searchTimeMs,
            long llmTimeMs,
            long totalTimeMs,
            RagPerformanceValidation performance
    ) {
        public static RagMetadata of(long embedding, long search, long llm, long total) {
            return new RagMetadata(embedding, search, llm, total, null);
        }
        
        public static RagMetadata of(long embedding, long search, long llm, long total, RagPerformanceValidation performance) {
            return new RagMetadata(embedding, search, llm, total, performance);
        }
    }

    public record RagResponse(
            boolean success,
            String question,
            String answer,
            List<RagContext> contexts,
            RagMetadata metadata,
            String error
    ) {
        public static RagResponse success(String question, String answer, List<RagContext> contexts, RagMetadata metadata) {
            return new RagResponse(true, question, answer, contexts, metadata, null);
        }

        public static RagResponse error(String error) {
            return new RagResponse(false, null, null, null, null, error);
        }
    }

    public record RagContext(String id, String msgType, double score, Map<String, String> mapping) {}
}
