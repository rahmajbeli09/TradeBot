package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsingResult {
    
    private Map<String, ParsedFeedGroup> groupsByMsgType;
    private List<String> parsingErrors;
    private int totalLinesProcessed;
    private int validLinesProcessed;
    private int errorLinesCount;
    private LocalDateTime parsedAt;
    private String sourceFileName;

    public ParsingResult(Map<String, ParsedFeedGroup> groupsByMsgType, List<String> parsingErrors, 
                        int totalLinesProcessed, int validLinesProcessed, String sourceFileName) {
        this.groupsByMsgType = groupsByMsgType;
        this.parsingErrors = parsingErrors;
        this.totalLinesProcessed = totalLinesProcessed;
        this.validLinesProcessed = validLinesProcessed;
        this.errorLinesCount = totalLinesProcessed - validLinesProcessed;
        this.parsedAt = LocalDateTime.now();
        this.sourceFileName = sourceFileName;
    }

    public int getGroupCount() {
        return groupsByMsgType != null ? groupsByMsgType.size() : 0;
    }

    public boolean hasErrors() {
        return parsingErrors != null && !parsingErrors.isEmpty();
    }

    public double getSuccessRate() {
        return totalLinesProcessed > 0 ? (validLinesProcessed * 100.0 / totalLinesProcessed) : 0.0;
    }
}
