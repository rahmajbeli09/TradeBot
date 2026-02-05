package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    
    private String msgType;
    private Map<String, String> mapping;
    private LocalDateTime analyzedAt;
    private String originalLine;
    private String anonymizedLine;
    private int fieldCount;

    public FieldMapping(String msgType, Map<String, String> mapping, String originalLine, 
                     String anonymizedLine, int fieldCount) {
        this.msgType = msgType;
        this.mapping = mapping;
        this.originalLine = originalLine;
        this.anonymizedLine = anonymizedLine;
        this.fieldCount = fieldCount;
        this.analyzedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return msgType != null && !msgType.trim().isEmpty() 
                && mapping != null && !mapping.isEmpty();
    }

    public int getMappingFieldCount() {
        return mapping != null ? mapping.size() : 0;
    }
}
