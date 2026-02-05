package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawFeedLine {
    
    private String content;
    private int lineNumber;
    private String sourceFileName;
    private LocalDateTime readAt;

    public RawFeedLine(String content, int lineNumber, String sourceFileName) {
        this.content = content;
        this.lineNumber = lineNumber;
        this.sourceFileName = sourceFileName;
        this.readAt = LocalDateTime.now();
    }

    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }

    public String getTrimmedContent() {
        return content != null ? content.trim() : "";
    }

    public boolean isValid() {
        return !isEmpty();
    }
}
