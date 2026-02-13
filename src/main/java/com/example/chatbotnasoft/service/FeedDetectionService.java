package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.Feed;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.entity.MappingStatus;
import com.example.chatbotnasoft.repository.FeedRepository;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedDetectionService {

    private final FeedRepository feedRepository;
    private final FeedMappingRepository feedMappingRepository;

    public record FeedValidationResult(
            boolean isValid,
            String errorMessage
    ) {
        public static FeedValidationResult valid() {
            return new FeedValidationResult(true, null);
        }

        public static FeedValidationResult corrupted(String errorMessage) {
            return new FeedValidationResult(false, errorMessage);
        }
    }

    public boolean isMsgTypeKnown(String msgType) {
        log.debug("🔍 Vérification du msg-type dans MongoDB: '{}'", msgType);

        Optional<FeedMapping> mappingOpt = feedMappingRepository.findByMsgType(msgType);
        boolean isKnown = mappingOpt.isPresent() && MappingStatus.VALIDE.equals(mappingOpt.get().getStatus());
        
        if (isKnown) {
            log.debug("✅ Msg-type '{}' connu dans la base de données", msgType);
        } else {
            log.info("❌ Msg-type '{}' inconnu - anonymisation requise", msgType);
        }
        
        return isKnown;
    }

    public List<String> getKnownMsgTypes(List<String> msgTypes) {
        log.debug("🔍 Recherche de {} msg-types dans la base de données", msgTypes.size());

        List<String> knownMsgTypes = msgTypes.stream()
                .filter(msgType -> {
                    Optional<FeedMapping> mappingOpt = feedMappingRepository.findByMsgType(msgType);
                    return mappingOpt.isPresent() && MappingStatus.VALIDE.equals(mappingOpt.get().getStatus());
                })
                .toList();
        
        log.debug("✅ {} msg-types connus sur {} recherchés", knownMsgTypes.size(), msgTypes.size());
        
        return knownMsgTypes;
    }

    public FeedValidationResult validateFeedLine(String feedLine, int lineNumber) {
        log.debug("🔍 Validation ligne {} : '{}'", lineNumber, feedLine);
        
        // Validation 1: Ligne vide ou null
        if (feedLine == null || feedLine.trim().isEmpty()) {
            logError("FEED_VIDE", "Ligne vide détectée", Map.of("lineNumber", lineNumber, "content", feedLine));
            return FeedValidationResult.corrupted("Ligne vide détectée");
        }
        
        // Validation 2: Format minimum requis (au moins 3 champs séparés par '|')
        String[] fields = feedLine.split("\\|");
        if (fields.length < 3) {
            logError("FEED_MAL_FORMATE", "Format incorrect", Map.of("lineNumber", lineNumber, "fields", fields.length, "content", feedLine));
            return FeedValidationResult.corrupted("Format incorrect : moins de 3 champs");
        }
        
        // Validation 3: msgType présent et non vide
        String msgType = fields.length > 0 ? fields[0].trim() : "";
        if (msgType.isEmpty()) {
            logError("FEED_MSGTYPE_VIDE", "msgType manquant", Map.of("lineNumber", lineNumber, "content", feedLine));
            return FeedValidationResult.corrupted("msgType manquant");
        }
        
        // Validation 4: Caractères non valides
        if (feedLine.matches(".*[^a-zA-Z0-9|\\s\\-_:.,éèêëàâäôöùûüîïç].*")) {
            logError("FEED_CARACTERES_INVALIDES", "Caractères non valides", Map.of("lineNumber", lineNumber, "content", feedLine));
            return FeedValidationResult.corrupted("Caractères non valides détectés");
        }
        
        log.debug("✅ Ligne {} valide", lineNumber);
        return FeedValidationResult.valid();
    }

    private void logError(String errorType, String message, Map<String, Object> payload) {
        log.error("❌ ERREUR FEED [{}] : {} | Payload: {}", errorType, message, payload);
    }

    public Feed createFeedType(String msgType, String description) {
        log.info("📝 Création d'un nouveau msg-type: '{}' - '{}'", msgType, description);
        
        Feed newFeed = new Feed(msgType, description);
        Feed saved = feedRepository.save(newFeed);
        
        log.info("✅ Msg-type '{}' créé avec ID: {}", msgType, saved.getId());
        return saved;
    }

    public List<Feed> getAllActiveFeeds() {
        return feedRepository.findByIsActive(true);
    }

    public long countActiveFeeds() {
        return feedRepository.findByIsActive(true).size();
    }
}
