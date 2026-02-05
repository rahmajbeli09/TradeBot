package com.example.chatbotnasoft.repository;

import com.example.chatbotnasoft.entity.MessageType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTypeRepository extends MongoRepository<MessageType, String> {

    Optional<MessageType> findByTypeName(String typeName);

    List<MessageType> findByDescriptionContainingIgnoreCase(String description);

    @Query(value = "{}", count = true)
    long countAllMessageTypes();

    boolean existsByTypeName(String typeName);
}
