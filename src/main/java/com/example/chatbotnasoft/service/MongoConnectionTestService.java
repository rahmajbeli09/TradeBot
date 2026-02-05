package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.MessageType;
import com.example.chatbotnasoft.repository.MessageTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MongoConnectionTestService {

    private final MessageTypeRepository messageTypeRepository;
    private final MongoTemplate mongoTemplate;

    public boolean testConnection() {
        try {
            log.info("Test de connexion à MongoDB...");
            
            // Test basique : compter les collections
            List<String> collections = mongoTemplate.getCollectionNames().stream().toList();
            log.info("Collections trouvées: {}", collections);
            
            // Test d'insertion et lecture
            MessageType testType = new MessageType("TEST_TYPE", "Type de message de test");
            MessageType saved = messageTypeRepository.save(testType);
            log.info("Message type sauvegardé: {}", saved);
            
            // Test de lecture
            Optional<MessageType> found = messageTypeRepository.findById(saved.getId());
            if (found.isPresent()) {
                log.info("Message type trouvé: {}", found.get());
                
                // Nettoyage
                messageTypeRepository.deleteById(saved.getId());
                log.info("Test nettoyé avec succès");
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Erreur lors du test de connexion à MongoDB", e);
            return false;
        }
    }

    public long getMessageTypeCount() {
        return messageTypeRepository.count();
    }

    public List<MessageType> getAllMessageTypes() {
        return messageTypeRepository.findAll();
    }

    public MessageType saveMessageType(MessageType messageType) {
        if (messageType.getCreatedAt() == null) {
            messageType.setCreatedAt(LocalDateTime.now());
        }
        messageType.setUpdatedAt(LocalDateTime.now());
        return messageTypeRepository.save(messageType);
    }
}
