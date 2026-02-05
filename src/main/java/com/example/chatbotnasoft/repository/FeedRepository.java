package com.example.chatbotnasoft.repository;

import com.example.chatbotnasoft.entity.Feed;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedRepository extends MongoRepository<Feed, String> {

    Optional<Feed> findByMsgTypeAndIsActive(String msgType, Boolean isActive);

    List<Feed> findByMsgTypeContainingIgnoreCase(String msgType);

    List<Feed> findByIsActive(Boolean isActive);

    @Query(value = "{ 'msgType': ?0, 'isActive': true }", count = true)
    long countByMsgTypeAndIsActive(String msgType);

    boolean existsByMsgTypeAndIsActive(String msgType, Boolean isActive);

    List<Feed> findByMsgTypeInAndIsActive(List<String> msgTypes, Boolean isActive);
}
