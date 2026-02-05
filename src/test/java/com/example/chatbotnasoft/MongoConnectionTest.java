package com.example.chatbotnasoft;

import com.example.chatbotnasoft.entity.MessageType;
import com.example.chatbotnasoft.repository.MessageTypeRepository;
import com.example.chatbotnasoft.service.MongoConnectionTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/chatbot_test"
})
class MongoConnectionTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MessageTypeRepository messageTypeRepository;

    @Autowired
    private MongoConnectionTestService mongoConnectionTestService;

    @BeforeEach
    void setUp() {
        // Nettoyer la collection avant chaque test
        messageTypeRepository.deleteAll();
    }

    @Test
    void testMongoConnection() {
        // Test de connexion basique
        assertNotNull(mongoTemplate);
        assertNotNull(messageTypeRepository);
        
        // Vérifier que la connexion fonctionne
        assertTrue(mongoConnectionTestService.testConnection());
    }

    @Test
    void testSaveAndFindMessageType() {
        // Créer un MessageType
        MessageType messageType = new MessageType("INFO", "Message d'information");
        MessageType saved = messageTypeRepository.save(messageType);

        // Vérifier l'enregistrement
        assertNotNull(saved.getId());
        assertEquals("INFO", saved.getTypeName());
        assertEquals("Message d'information", saved.getDescription());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        // Tester la recherche par ID
        Optional<MessageType> found = messageTypeRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getTypeName(), found.get().getTypeName());

        // Tester la recherche par type name
        Optional<MessageType> foundByTypeName = messageTypeRepository.findByTypeName("INFO");
        assertTrue(foundByTypeName.isPresent());
        assertEquals(saved.getId(), foundByTypeName.get().getId());
    }

    @Test
    void testFindAllMessageTypes() {
        // Créer plusieurs MessageTypes
        MessageType type1 = new MessageType("INFO", "Message d'information");
        MessageType type2 = new MessageType("ERROR", "Message d'erreur");
        MessageType type3 = new MessageType("WARNING", "Message d'avertissement");

        messageTypeRepository.save(type1);
        messageTypeRepository.save(type2);
        messageTypeRepository.save(type3);

        // Tester findAll
        List<MessageType> allTypes = messageTypeRepository.findAll();
        assertEquals(3, allTypes.size());

        // Tester le service
        List<MessageType> serviceTypes = mongoConnectionTestService.getAllMessageTypes();
        assertEquals(3, serviceTypes.size());
    }

    @Test
    void testCountAndExists() {
        assertEquals(0, messageTypeRepository.count());
        assertFalse(messageTypeRepository.existsByTypeName("INFO"));

        MessageType messageType = new MessageType("INFO", "Message d'information");
        messageTypeRepository.save(messageType);

        assertEquals(1, messageTypeRepository.count());
        assertTrue(messageTypeRepository.existsByTypeName("INFO"));
    }

    @Test
    void testUpdateTimestamp() {
        MessageType messageType = new MessageType("INFO", "Message d'information");
        MessageType saved = messageTypeRepository.save(messageType);
        
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();
        
        // Attendre un peu pour garantir une différence de temps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        saved.updateTimestamp();
        MessageType updated = messageTypeRepository.save(saved);
        
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt));
    }
}
