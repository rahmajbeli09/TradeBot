package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.ParsingResult;
import com.example.chatbotnasoft.dto.RawFeedLine;
import com.example.chatbotnasoft.service.FeedParsingService;
import com.example.chatbotnasoft.service.FileReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/parsing")
@RequiredArgsConstructor
@Slf4j
public class ParsingController {

    private final FeedParsingService feedParsingService;
    private final FileReadingService fileReadingService;

    @PostMapping("/parse-file/{fileName}")
    public ResponseEntity<Map<String, Object>> parseFile(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = Paths.get("input/feeds").resolve(fileName);
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "Fichier non trouvé: " + fileName);
                return ResponseEntity.notFound().build();
            }

            // Lire toutes les lignes
            List<RawFeedLine> lines;
            try (Stream<RawFeedLine> lineStream = fileReadingService.readFileLines(filePath)) {
                lines = lineStream.toList();
            }

            // Parser et regrouper
            ParsingResult parsingResult = feedParsingService.parseAndGroupLines(lines);
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("totalLines", parsingResult.getTotalLinesProcessed());
            response.put("validLines", parsingResult.getValidLinesProcessed());
            response.put("errorLines", parsingResult.getErrorLinesCount());
            response.put("groupCount", parsingResult.getGroupCount());
            response.put("successRate", parsingResult.getSuccessRate());
            response.put("groups", parsingResult.getGroupsByMsgType());
            response.put("errors", parsingResult.getParsingErrors());
            response.put("timestamp", parsingResult.getParsedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du parsing du fichier: {}", fileName, e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/validate-line")
    public ResponseEntity<Map<String, Object>> validateLine(@RequestParam String content) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = feedParsingService.isValidFeedLine(content);
            String msgType = null;
            
            if (isValid) {
                String[] fields = content.split(";");
                if (fields.length >= 2) {
                    msgType = fields[1].trim();
                }
            }
            
            response.put("success", true);
            response.put("content", content);
            response.put("isValid", isValid);
            response.put("msgType", msgType);
            response.put("fieldCount", content.split(";").length);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la validation de la ligne", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
