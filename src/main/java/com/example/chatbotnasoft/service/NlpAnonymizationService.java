package com.example.chatbotnasoft.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class NlpAnonymizationService {

    private static final Map<String, String> GENERIC_TOKENS = Map.of(
            "ID_TRANSACTION", "TRANSACTION_ID_X",
            "TRANSACTION_ID", "TRANSACTION_ID_X",
            "SKU", "ARTICLE_ID_X",
            "OPERATEUR", "OPERATEUR_ID_X",
            "ID_CLIENT", "CLIENT_ID_X",
            "CLIENT_ID", "CLIENT_ID_X",
            "REFERENCE", "REFERENCE_X",
            "REF", "REFERENCE_X"
    );

    private static final Map<Pattern, String> PATTERNS = Map.of(
            Pattern.compile("\\b[A-Z]{2,6}_\\d{4,}\\b", Pattern.CASE_INSENSITIVE), "CODE_X",
            Pattern.compile("\\b\\d{6,}\\b"), "NUMERO_X",
            Pattern.compile("\\b[A-Za-z0-9]{8,}\\b"), "ALPHANUM_X"
    );

    public String anonymize(String rawNlpText) {
        if (rawNlpText == null || rawNlpText.isBlank()) {
            return rawNlpText;
        }

        String anonymized = rawNlpText;

        anonymized = replaceKnownTokens(anonymized);
        anonymized = replacePatterns(anonymized);

        return anonymized;
    }

    private String replaceKnownTokens(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : GENERIC_TOKENS.entrySet()) {
            String key = entry.getKey();
            String replacement = entry.getValue();
            result = result.replaceAll("(?i)\\b" + Pattern.quote(key) + "\\b", replacement);
        }
        return result;
    }

    private String replacePatterns(String text) {
        String result = text;
        for (Map.Entry<Pattern, String> entry : PATTERNS.entrySet()) {
            Pattern pattern = entry.getKey();
            String replacement = entry.getValue();
            Matcher matcher = pattern.matcher(result);
            result = matcher.replaceAll(replacement);
        }
        return result;
    }
}
