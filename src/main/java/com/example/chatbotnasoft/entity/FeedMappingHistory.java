package com.example.chatbotnasoft.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feed_history")
public class FeedMappingHistory {

    @Id
    private String id;

    private String msgType;

    private int version;

    private MappingStatus status;

    private Map<String, String> mapping;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime archivedAt;

    public FeedMappingHistory(String msgType, int version, MappingStatus status, Map<String, String> mapping,
                              LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime archivedAt) {
        this.msgType = msgType;
        this.version = version;
        this.status = status;
        this.mapping = mapping;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;
    }
}
