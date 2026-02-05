package com.example.chatbotnasoft.repository;

import com.example.chatbotnasoft.entity.FeedMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedMappingRepository extends MongoRepository<FeedMapping, String> {
    
    /**
     * Rechercher un mapping par msg-type
     */
    Optional<FeedMapping> findByMsgType(String msgType);
    
    /**
     * Vérifier si un msg-type existe déjà
     */
    boolean existsByMsgType(String msgType);
    
    /**
     * Compter le nombre total de mappings
     */
    long count();
    
    /**
     * Supprimer un mapping par msg-type
     */
    void deleteByMsgType(String msgType);
}
