package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.FieldMapping;
import com.example.chatbotnasoft.entity.FeedMapping;
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
public class FeedMappingService {
    
    private final FeedMappingRepository feedMappingRepository;
    
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
                if (storeMappingIfNotExists(fieldMapping)) {
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
     * Stocke un mapping s'il n'existe pas déjà
     */
    private boolean storeMappingIfNotExists(FieldMapping fieldMapping) {
        String msgType = fieldMapping.getMsgType();
        
        // Vérifier si le msg-type existe déjà
        if (feedMappingRepository.existsByMsgType(msgType)) {
            log.debug("⏭️ Msg-type '{}' déjà existant, ignoré", msgType);
            return false;
        }
        
        // Créer et sauvegarder le nouveau mapping
        FeedMapping feedMapping = new FeedMapping(
                msgType,
                fieldMapping.getMapping()
        );
        
        if (feedMapping.isValid()) {
            FeedMapping saved = feedMappingRepository.save(feedMapping);
            log.info("✅ Mapping stocké pour msg-type '{}' avec {} champs (ID: {})", 
                    msgType, feedMapping.getFieldCount(), saved.getId());
            return true;
        } else {
            log.warn("⚠️ Mapping invalide pour msg-type '{}', non stocké", msgType);
            return false;
        }
    }
    
    /**
     * Récupérer un mapping par msg-type
     */
    public Optional<FeedMapping> getMappingByMsgType(String msgType) {
        return feedMappingRepository.findByMsgType(msgType);
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
