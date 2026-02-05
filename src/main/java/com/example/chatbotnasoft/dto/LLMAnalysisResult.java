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
public class LLMAnalysisResult {
    
    private Map<String, List<FieldMapping>> resultsByMsgType;
    private List<String> analysisErrors;
    private int totalLinesAnalyzed;
    private int successfulAnalyses;
    private int failedAnalyses;
    private LocalDateTime analyzedAt;
    private String sourceFileName;
    private double successRate;

    public LLMAnalysisResult(Map<String, List<FieldMapping>> resultsByMsgType, 
                          List<String> analysisErrors, int totalLinesAnalyzed, String sourceFileName) {
        this.resultsByMsgType = resultsByMsgType;
        this.analysisErrors = analysisErrors;
        this.totalLinesAnalyzed = totalLinesAnalyzed;
        this.sourceFileName = sourceFileName;
        this.analyzedAt = LocalDateTime.now();
        
        this.successfulAnalyses = resultsByMsgType.values().stream()
                .mapToInt(List::size)
                .sum();
        this.failedAnalyses = totalLinesAnalyzed - successfulAnalyses;
        this.successRate = totalLinesAnalyzed > 0 ? (successfulAnalyses * 100.0 / totalLinesAnalyzed) : 0.0;
    }

    public boolean hasErrors() {
        return analysisErrors != null && !analysisErrors.isEmpty();
    }

    public int getMsgTypeCount() {
        return resultsByMsgType != null ? resultsByMsgType.size() : 0;
    }

    public boolean isValid() {
        return getMsgTypeCount() > 0 && successfulAnalyses > 0;
    }
    
    public boolean hasSuccessfulMappings() {
        return successfulAnalyses > 0 && resultsByMsgType != null && !resultsByMsgType.isEmpty();
    }
    
    public List<FieldMapping> getMappings() {
        return resultsByMsgType != null ? 
                resultsByMsgType.values().stream()
                        .flatMap(List::stream)
                        .toList() : 
                List.of();
    }
}
