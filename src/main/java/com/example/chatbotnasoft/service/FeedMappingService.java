package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.FieldMapping;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.entity.FeedMappingHistory;
import com.example.chatbotnasoft.entity.MappingStatus;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import com.example.chatbotnasoft.repository.FeedMappingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedMappingService {
    
    private final FeedMappingRepository feedMappingRepository;

    private final FeedMappingHistoryRepository feedMappingHistoryRepository;
    
    /**
     * Stocke les mappings LLM en évitant les doublons
     */
    public void storeMappings(List<FieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            log.info("📋 Aucun mapping à stocker");
            return;
        }
        
        log.info("💾 Début du stockage de {} mappings", mappings.size());
        
        int storedCount = 0;
        int skippedCount = 0;
        
        for (FieldMapping fieldMapping : mappings) {
            try {
                if (storeMappingWithVersioning(fieldMapping)) {
                    storedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors du stockage du mapping pour msg-type {}: {}", 
                        fieldMapping.getMsgType(), e.getMessage());
            }
        }
        
        log.info("📊 Stockage terminé: {} mappings stockés, {} mappings ignorés (doublons)", 
                storedCount, skippedCount);
    }
    
    /**
     * Stocke un mapping avec historisation et versioning.
     * - Si le mapping n'existe pas: création version 1
     * - Si le mapping existe et diffère: archive l'ancienne version dans feed_history et incrémente version
     * - Si le mapping existe et identique: ignoré
     */
    private boolean storeMappingWithVersioning(FieldMapping fieldMapping) {
        String msgType = fieldMapping.getMsgType();

        if (msgType == null || msgType.trim().isEmpty()) {
            log.warn("⚠️ Msg-type vide, mapping ignoré");
            return false;
        }
        
        Map<String, String> mapping = fieldMapping.getMapping();

        if (mapping == null || mapping.isEmpty()) {
            log.warn("⚠️ Mapping vide pour msg-type '{}', non stocké", msgType);
            return false;
        }

        MappingStatus status = determineMappingStatus(mapping);

        Optional<FeedMapping> existingOpt = feedMappingRepository.findByMsgType(msgType);
        if (existingOpt.isEmpty()) {
            FeedMapping created = new FeedMapping(msgType, mapping);
            created.setStatus(status);
            created.setVersion(1);
            created.setIsActive(true);
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());

            FeedMapping saved = feedMappingRepository.save(created);
            log.info("✅ Mapping créé pour msg-type '{}' (v{}) statut '{}' (ID: {})", 
                    msgType, saved.getVersion(), saved.getStatus().getLabel(), saved.getId());
            return true;
        }

        FeedMapping existing = existingOpt.get();

        // Règle métier: un mapping Validé est figé (pas de modification / pas de versioning)
        if (MappingStatus.VALIDE.equals(existing.getStatus())) {
            log.info("🔒 Mapping '{}' déjà Validé (v{}): modification ignorée", msgType, existing.getVersion());
            return false;
        }

        if (existing.getMapping() != null && existing.getMapping().equals(mapping)) {
            log.debug("⏭️ Mapping inchangé pour msg-type '{}' (v{}), ignoré", msgType, existing.getVersion());
            return false;
        }

        archiveExisting(existing);

        existing.setMapping(mapping);
        existing.setStatus(status);
        existing.setVersion(existing.getVersion() + 1);
        existing.setIsActive(true);
        existing.setUpdatedAt(LocalDateTime.now());

        FeedMapping saved = feedMappingRepository.save(existing);
        log.info("✅ Mapping mis à jour pour msg-type '{}' (v{}) statut '{}' (ID: {})", 
                msgType, saved.getVersion(), saved.getStatus().getLabel(), saved.getId());
        return true;
    }

    private void archiveExisting(FeedMapping existing) {
        FeedMappingHistory history = new FeedMappingHistory(
                existing.getMsgType(),
                existing.getVersion(),
                existing.getStatus(),
                existing.getMapping(),
                existing.getCreatedAt(),
                existing.getUpdatedAt(),
                LocalDateTime.now()
        );
        feedMappingHistoryRepository.save(history);
    }

    private MappingStatus determineMappingStatus(Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return MappingStatus.INCOMPLET;
        }

        long total = mapping.size();

        long noMeaningCount = mapping.values().stream()
                .filter(this::isNoMeaning)
                .count();

        if (noMeaningCount == total) {
            return MappingStatus.INCOMPLET;
        }

        boolean hasSpecialOrAnonymized = mapping.values().stream()
                .anyMatch(this::isSpecialOrAnonymized);

        if (hasSpecialOrAnonymized) {
            return MappingStatus.A_VERIFIER;
        }

        return MappingStatus.VALIDE;
    }

    private boolean isNoMeaning(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return true;
        }
        String lower = v.toLowerCase();
        return lower.equals("unknown")
                || lower.equals("valeur inconnue")
                || lower.contains("signification manquante");
    }

    private boolean isSpecialOrAnonymized(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return true;
        }
        String lower = v.toLowerCase();
        return v.contains("xxxxx")
                || lower.contains("donnée anonymisée")
                || lower.contains("valeur inconnue")
                || lower.contains("signification manquante")
                || lower.equals("unknown");
    }
    
    /**
     * Récupérer un mapping par msg-type
     */
    public Optional<FeedMapping> getMappingByMsgType(String msgType) {
        return feedMappingRepository.findByMsgType(msgType);
    }

    public List<FeedMappingHistory> getMappingHistory(String msgType) {
        return feedMappingHistoryRepository.findByMsgTypeOrderByVersionDesc(msgType);
    }

    public Optional<MappingStatus> getCurrentStatus(String msgType) {
        return feedMappingRepository.findByMsgType(msgType).map(FeedMapping::getStatus);
    }
    
    /**
     * Lister tous les mappings
     */
    public List<FeedMapping> getAllMappings() {
        return feedMappingRepository.findAll();
    }
    
    /**
     * Compter le nombre total de mappings
     */
    public long getTotalMappingsCount() {
        return feedMappingRepository.count();
    }
    
    /**
     * Supprimer un mapping par msg-type
     */
    public boolean deleteMappingByMsgType(String msgType) {
        if (feedMappingRepository.existsByMsgType(msgType)) {
            feedMappingRepository.deleteByMsgType(msgType);
            log.info("🗑️ Mapping supprimé pour msg-type '{}'", msgType);
            return true;
        }
        return false;
    }
}
