package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.entity.FeedMappingHistory;
import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.entity.MappingStatus;
import com.example.chatbotnasoft.service.FeedMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mappings")
@RequiredArgsConstructor
@Slf4j
public class FeedMappingController {
    
    private final FeedMappingService feedMappingService;
    
    /**
     * Récupérer tous les mappings
     */
    @GetMapping
    public ResponseEntity<List<FeedMapping>> getAllMappings() {
        List<FeedMapping> mappings = feedMappingService.getAllMappings();
        log.info("📋 Récupération de {} mappings", mappings.size());
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Récupérer un mapping par msg-type
     */
    @GetMapping("/{msgType}")
    public ResponseEntity<FeedMapping> getMappingByMsgType(@PathVariable String msgType) {
        Optional<FeedMapping> mapping = feedMappingService.getMappingByMsgType(msgType);
        
        if (mapping.isPresent()) {
            log.info("🔍 Mapping trouvé pour msg-type '{}'", msgType);
            return ResponseEntity.ok(mapping.get());
        } else {
            log.warn("⚠️ Aucun mapping trouvé pour msg-type '{}'", msgType);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{msgType}/status")
    public ResponseEntity<?> getMappingStatus(@PathVariable String msgType) {
        Optional<FeedMapping> mappingOpt = feedMappingService.getMappingByMsgType(msgType);
        if (mappingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FeedMapping mapping = mappingOpt.get();
        MappingStatus status = mapping.getStatus();

        var response = Map.of(
                "msgType", mapping.getMsgType(),
                "version", mapping.getVersion(),
                "status", status != null ? status.getLabel() : null,
                "updatedAt", mapping.getUpdatedAt(),
                "createdAt", mapping.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{msgType}/history")
    public ResponseEntity<List<FeedMappingHistory>> getMappingHistory(@PathVariable String msgType) {
        List<FeedMappingHistory> history = feedMappingService.getMappingHistory(msgType);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Supprimer un mapping par msg-type
     */
    @DeleteMapping("/{msgType}")
    public ResponseEntity<Void> deleteMappingByMsgType(@PathVariable String msgType) {
        boolean deleted = feedMappingService.deleteMappingByMsgType(msgType);
        
        if (deleted) {
            log.info("🗑️ Mapping supprimé pour msg-type '{}'", msgType);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("⚠️ Aucun mapping à supprimer pour msg-type '{}'", msgType);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtenir des statistiques sur les mappings
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getMappingStats() {
        long totalMappings = feedMappingService.getTotalMappingsCount();
        
        var stats = Map.of(
                "totalMappings", totalMappings,
                "description", "Statistiques des mappings de msg-types"
        );
        
        log.info("📊 Statistiques des mappings: {} mappings au total", totalMappings);
        return ResponseEntity.ok(stats);
    }
}
