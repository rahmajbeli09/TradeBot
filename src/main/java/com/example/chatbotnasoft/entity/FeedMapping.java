package com.example.chatbotnasoft.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.chatbotnasoft.entity.MappingStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feed")
public class FeedMapping {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String msgType;

    private int version;

    private MappingStatus status;

    private Map<String, String> mapping;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isActive;
    
    public FeedMapping(String msgType, Map<String, String> mapping) {
        this.msgType = msgType;
        this.mapping = mapping;
        this.version = 1;
        this.status = MappingStatus.INCOMPLET;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isValid() {
        return msgType != null && !msgType.trim().isEmpty() 
                && mapping != null && !mapping.isEmpty();
    }
    
    public int getFieldCount() {
        return mapping != null ? mapping.size() : 0;
    }
}
