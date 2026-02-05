package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedFeedGroup {
    
    private String msgType;
    private List<RawFeedLine> lines;
    private LocalDateTime parsedAt;
    private int totalLines;
    private String sourceFileName;

    public ParsedFeedGroup(String msgType, List<RawFeedLine> lines) {
        this.msgType = msgType;
        this.lines = lines;
        this.parsedAt = LocalDateTime.now();
        this.totalLines = lines.size();
        
        // Extraire le nom du fichier source (toutes les lignes viennent du même fichier)
        if (!lines.isEmpty()) {
            this.sourceFileName = lines.get(0).getSourceFileName();
        }
    }

    public boolean isEmpty() {
        return lines == null || lines.isEmpty();
    }

    public RawFeedLine getFirstLine() {
        return isEmpty() ? null : lines.get(0);
    }

    public RawFeedLine getLastLine() {
        return isEmpty() ? null : lines.get(lines.size() - 1);
    }

    public boolean isValid() {
        return msgType != null && !msgType.trim().isEmpty() && !isEmpty();
    }

    public void addLine(RawFeedLine line) {
        lines.add(line);
        totalLines = lines.size();
    }
}
