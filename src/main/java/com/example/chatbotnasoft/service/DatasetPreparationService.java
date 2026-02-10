package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.DatasetDocument;
import com.example.chatbotnasoft.dto.DatasetMetadata;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.entity.MappingStatus;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DatasetPreparationService {

    private final FeedMappingRepository feedMappingRepository;

    public List<DatasetDocument> buildDataset() {
        List<FeedMapping> mappings = feedMappingRepository.findByStatusIn(List.of(MappingStatus.VALIDE));

        return mappings.stream().map(this::toDatasetDocument).toList();
    }

    public String buildCsv() {
        List<DatasetDocument> docs = buildDataset();

        StringBuilder sb = new StringBuilder();
        sb.append("id,msgType,text,status,version,category,agent\n");

        for (DatasetDocument doc : docs) {
            String id = escapeCsv(doc.getId());
            String msgType = escapeCsv(doc.getMsgType());
            String text = escapeCsv(doc.getText());
            String status = escapeCsv(doc.getMetadata() != null ? doc.getMetadata().getStatus() : "");
            String version = String.valueOf(doc.getMetadata() != null ? doc.getMetadata().getVersion() : 0);
            String category = escapeCsv(doc.getMetadata() != null ? doc.getMetadata().getCategory() : "");
            String agent = escapeCsv(doc.getMetadata() != null ? doc.getMetadata().getAgent() : "");

            sb.append(id).append(',')
                    .append(msgType).append(',')
                    .append(text).append(',')
                    .append(status).append(',')
                    .append(version).append(',')
                    .append(category).append(',')
                    .append(agent).append('\n');
        }

        return sb.toString();
    }

    private DatasetDocument toDatasetDocument(FeedMapping mapping) {
        String msgType = mapping.getMsgType() != null ? mapping.getMsgType() : "";
        String id = !msgType.isBlank() ? msgType : mapping.getId();

        String text = buildText(mapping);
        String category = categorize(mapping);
        String agent = null;

        DatasetMetadata metadata = new DatasetMetadata(
                mapping.getStatus() != null ? mapping.getStatus().getLabel() : null,
                mapping.getVersion(),
                msgType,
                category,
                agent
        );

        return new DatasetDocument(id, msgType, text, metadata);
    }

    private String buildText(FeedMapping mapping) {
        if (mapping.getMapping() == null || mapping.getMapping().isEmpty()) {
            return normalizeText("MsgType " + mapping.getMsgType() + " :");
        }

        List<Map.Entry<String, String>> ordered = mapping.getMapping().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> extractChampIndex(e.getKey())))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("MsgType ").append(mapping.getMsgType() != null ? mapping.getMsgType().trim() : "").append(" : ");

        for (Map.Entry<String, String> e : ordered) {
            String signification = cleanSignification(e.getValue());
            if (signification == null) {
                continue;
            }

            String champ = e.getKey() != null ? e.getKey().trim() : "";
            if (!champ.isEmpty()) {
                sb.append(champ).append(" = ");
            }
            sb.append(signification).append(", ");
        }

        String raw = sb.toString().trim();
        if (raw.endsWith(",")) {
            raw = raw.substring(0, raw.length() - 1).trim();
        }
        return normalizeText(raw);
    }

    private String cleanSignification(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }

        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.equals("unknown")
                || lower.equals("valeur inconnue")
                || lower.contains("signification manquante")
                || lower.contains("donnée anonymisée")
                || lower.contains("xxxxx")) {
            return null;
        }

        v = v.replaceAll("\\s+", " ");
        return v;
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }

        String normalized = input;

        normalized = normalized.replaceAll("\\b(19|20)\\d{2}[/-]?(0[1-9]|1[0-2])[/-]?(0[1-9]|[12]\\d|3[01])\\b", "YYYYMMDD");
        normalized = normalized.replaceAll("\\b([01]\\d|2[0-3])[0-5]\\d[0-5]\\d\\b", "HHMMSS");
        normalized = normalized.replaceAll("\\b\\d+(?:[.,]\\d+)?\\b", "NUM");

        normalized = normalized.replace('\u00A0', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        return normalized;
    }

    private int extractChampIndex(String key) {
        if (key == null) {
            return Integer.MAX_VALUE;
        }
        java.util.regex.Matcher m = Pattern.compile("(\\d+)").matcher(key);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    private String categorize(FeedMapping mapping) {
        String text = buildText(mapping).toLowerCase(Locale.ROOT);

        if (text.contains("volume") || text.contains("quantite") || text.contains("quantité")) {
            return "Market Volumes";
        }
        if (text.contains("cours") || text.contains("prix") || text.contains("index")) {
            return "Market Prices";
        }
        if (text.contains("statut") || text.contains("status")) {
            return "Market Status";
        }
        if (text.contains("transaction") || text.contains("référence") || text.contains("reference")) {
            return "Transactions";
        }
        return "General";
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\"", "\"\"");
        return '"' + v + '"';
    }
}
