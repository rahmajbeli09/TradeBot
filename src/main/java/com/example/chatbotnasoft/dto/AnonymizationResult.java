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
public class AnonymizationResult {
    
    private Map<String, List<AnonymizedLine>> resultsByMsgType;
    private List<String> unknownMsgTypes;
    private List<String> knownMsgTypes;
    private int totalLinesProcessed;
    private int anonymizedLinesCount;
    private int nonAnonymizedLinesCount;
    private LocalDateTime anonymizedAt;
    private String sourceFileName;

    public AnonymizationResult(Map<String, List<AnonymizedLine>> resultsByMsgType, 
                           List<String> unknownMsgTypes, List<String> knownMsgTypes,
                           int totalLinesProcessed, String sourceFileName) {
        this.resultsByMsgType = resultsByMsgType;
        this.unknownMsgTypes = unknownMsgTypes;
        this.knownMsgTypes = knownMsgTypes;
        this.totalLinesProcessed = totalLinesProcessed;
        this.anonymizedLinesCount = unknownMsgTypes.stream()
                .mapToInt(msgType -> resultsByMsgType.get(msgType).size())
                .sum();
        this.nonAnonymizedLinesCount = totalLinesProcessed - anonymizedLinesCount;
        this.anonymizedAt = LocalDateTime.now();
        this.sourceFileName = sourceFileName;
    }

    public int getUnknownMsgTypesCount() {
        return unknownMsgTypes != null ? unknownMsgTypes.size() : 0;
    }

    public int getKnownMsgTypesCount() {
        return knownMsgTypes != null ? knownMsgTypes.size() : 0;
    }

    public double getAnonymizationRate() {
        return totalLinesProcessed > 0 ? (anonymizedLinesCount * 100.0 / totalLinesProcessed) : 0.0;
    }

    public boolean hasUnknownMsgTypes() {
        return getUnknownMsgTypesCount() > 0;
    }
}
