package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.config.QdrantProperties;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.entity.MappingStatus;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingIndexationService {

    private final FeedMappingRepository feedMappingRepository;
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;
    private final NlpAnonymizationService nlpAnonymizationService;
    private final ObjectMapper objectMapper;

    public IndexationResult indexAllValidatedActive() {
        List<FeedMapping> mappings = feedMappingRepository.findByStatusAndIsActive(MappingStatus.VALIDE, true);

        log.info("🚀 Indexation embeddings: démarrage ({} documents à traiter)", mappings.size());

        int indexed = 0;
        int skipped = 0;
        int failed = 0;

        Integer vectorSize = null;
        List<Map<String, Object>> batch = new ArrayList<>();
        int batchSize = Math.max(1, qdrantProperties.getIndexBatchSize());

        for (FeedMapping mapping : mappings) {
            try {
                String rawText = buildEmbeddingText(mapping);
                String anonymizedText = nlpAnonymizationService.anonymize(rawText);
                List<Double> vector = geminiEmbeddingService.embed(anonymizedText);

                if (vector == null || vector.isEmpty()) {
                    failed++;
                    log.warn("⚠️ Embedding échoué (vector null/vide) pour msgType '{}' (id={})", mapping.getMsgType(), mapping.getId());
                    continue;
                }

                if (vectorSize == null) {
                    vectorSize = vector.size();
                    qdrantClient.ensureCollectionExists(vectorSize);
                    log.info("📐 Dimension embedding détectée: {}", vectorSize);
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("msgType", mapping.getMsgType());
                payload.put("fieldCount", mapping.getFieldCount());
                payload.put("version", mapping.getVersion());
                payload.put("status", mapping.getStatus() != null ? mapping.getStatus().getLabel() : null);
                payload.put("createdAt", mapping.getCreatedAt());
                payload.put("updatedAt", mapping.getUpdatedAt());

                Map<String, Object> point = new HashMap<>();
                point.put("id", toUuid(mapping.getId()));
                point.put("vector", vector);
                point.put("payload", payload);

                batch.add(point);

                if (batch.size() >= batchSize) {
                    qdrantClient.upsertPoints(batch);
                    indexed += batch.size();
                    batch.clear();
                }

            } catch (Exception e) {
                failed++;
                log.error("❌ Erreur indexation pour msgType '{}' (id={}): {}", mapping.getMsgType(), mapping.getId(), e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            try {
                qdrantClient.upsertPoints(batch);
                indexed += batch.size();
            } catch (Exception e) {
                failed += batch.size();
                log.error("❌ Erreur upsert batch final: {}", e.getMessage());
            }
        }

        log.info("✅ Indexation embeddings terminée: indexed={}, skipped={}, failed={}", indexed, skipped, failed);
        return new IndexationResult(mappings.size(), indexed, skipped, failed, vectorSize);
    }

    private String buildEmbeddingText(FeedMapping mapping) {
        String msgType = mapping.getMsgType() != null ? mapping.getMsgType().trim() : "";
        int fieldCount = mapping.getFieldCount();

        StringBuilder sb = new StringBuilder();
        sb.append("Message de type ").append(msgType).append(" contenant ").append(fieldCount).append(" champs :\n");

        if (mapping.getMapping() == null || mapping.getMapping().isEmpty()) {
            return sb.toString();
        }

        mapping.getMapping().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> extractChampIndex(e.getKey())))
                .forEach(e -> {
                    String champ = e.getKey() != null ? e.getKey().trim() : "";
                    String signification = clean(e.getValue());
                    if (signification.isEmpty()) {
                        return;
                    }
                    sb.append(champ).append(" : ").append(signification).append("\n");
                });

        return sb.toString().trim();
    }

    private String toUuid(String source) {
        if (source == null) {
            return UUID.randomUUID().toString();
        }
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private int extractChampIndex(String key) {
        if (key == null) {
            return Integer.MAX_VALUE;
        }
        Matcher m = Pattern.compile("(\\d+)").matcher(key);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return "";
        }
        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.contains("donnée anonymisée") || lower.contains("signification manquante") || lower.equals("unknown") || lower.equals("valeur inconnue")) {
            return "";
        }
        return v.replaceAll("\\s+", " ");
    }

    public record IndexationResult(int total, int indexed, int skipped, int failed, Integer vectorSize) {}

    public List<Map<String, Object>> searchByQuery(String query, int limit) {
        log.info("🔎 QUERY: {}", query);
        List<Double> queryVector = geminiEmbeddingService.embed(query);
        log.info("🔎 QUERY VECTOR SIZE: {}", queryVector != null ? queryVector.size() : "null");
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }

        String searchResult = qdrantClient.search(queryVector, limit);
        log.info("🔎 QDRANT RAW RESULT: {}", searchResult);
        if (searchResult == null || searchResult.isBlank()) {
            return List.of();
        }

        return extractMappingsFromSearchResult(searchResult);
    }

    private List<Map<String, Object>> extractMappingsFromSearchResult(String searchResult) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(searchResult);
            JsonNode points = root.path("result");
            log.info("🔎 POINTS RAW: {}", points.toString());
            for (JsonNode point : points) {
                JsonNode payload = point.path("payload");
                log.info("🔎 PAYLOAD RAW: {}", payload.toString());
                String msgType = payload.path("msgType").asText();
                log.info("🔎 MSGTYPE EXTRACTED: '{}'", msgType);
                if (msgType.isBlank()) continue;

                FeedMapping mapping = feedMappingRepository.findByMsgTypeAndIsActive(msgType, true);
                if (mapping != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("msgType", mapping.getMsgType());
                    result.put("mapping", mapping.getMapping());
                    result.put("version", mapping.getVersion());
                    result.put("status", mapping.getStatus() != null ? mapping.getStatus().getLabel() : null);
                    results.add(result);
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur extraction mappings depuis Qdrant search: {}", e.getMessage());
        }
        return results;
    }
}
