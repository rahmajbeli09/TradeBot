package com.example.chatbotnasoft.repository;

import com.example.chatbotnasoft.entity.FeedMappingHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedMappingHistoryRepository extends MongoRepository<FeedMappingHistory, String> {

    List<FeedMappingHistory> findByMsgTypeOrderByVersionDesc(String msgType);
}
