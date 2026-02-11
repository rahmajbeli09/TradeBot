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
            return RagResponse.error("Aucun mapping valide trouvé");
        }

        // 4. Construction du contexte
        String contextText = buildContext(contexts);

        // 5. Génération de la réponse
        long llmStart = System.currentTimeMillis();
        String prompt = buildPrompt(question, contextText);
        String answer = simpleLlmService.generate(question, contextText);
        long llmTime = System.currentTimeMillis() - llmStart;

        if (answer == null || answer.isBlank()) {
            return RagResponse.error("Impossible de générer une réponse");
        }

        long totalTime = System.currentTimeMillis() - startTime;

        return RagResponse.success(
                question,
                answer.trim(),
                contexts,
                RagMetadata.of(embeddingTime, searchTime, llmTime, totalTime)
        );
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

    private String buildContext(List<RagContext> contexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contexte disponible :\n");
        for (RagContext ctx : contexts) {
            sb.append(String.format("MsgType %s (score: %.2f) :\n", ctx.msgType(), ctx.score()));
            ctx.mapping().forEach((champ, signification) ->
                    sb.append(String.format("- %s : %s\n", champ, signification))
            );
            sb.append("\n");
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

    public record RagMetadata(
            long embeddingTimeMs,
            long searchTimeMs,
            long llmTimeMs,
            long totalTimeMs
    ) {
        public static RagMetadata of(long embedding, long search, long llm, long total) {
            return new RagMetadata(embedding, search, llm, total);
        }
    }
}
