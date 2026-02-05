package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnonymizedLine {
    
    private String originalLine;
    private String anonymizedLine;
    private String msgType;
    private int lineNumber;
    private String sourceFileName;
    private LocalDateTime anonymizedAt;
    private boolean wasAnonymized;

    public AnonymizedLine(String originalLine, String anonymizedLine, String msgType, 
                         int lineNumber, String sourceFileName, boolean wasAnonymized) {
        this.originalLine = originalLine;
        this.anonymizedLine = anonymizedLine;
        this.msgType = msgType;
        this.lineNumber = lineNumber;
        this.sourceFileName = sourceFileName;
        this.wasAnonymized = wasAnonymized;
        this.anonymizedAt = LocalDateTime.now();
    }

    public String getTrimmedOriginalLine() {
        return originalLine != null ? originalLine.trim() : "";
    }

    public String getTrimmedAnonymizedLine() {
        return anonymizedLine != null ? anonymizedLine.trim() : "";
    }
}
