package com.example.chatbotnasoft.controller;

import com.example.chatbotnasoft.dto.DatasetDocument;
import com.example.chatbotnasoft.service.DatasetPreparationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetPreparationService datasetPreparationService;

    @GetMapping("/mappings")
    public ResponseEntity<?> exportMappingsDataset(
            @RequestParam(defaultValue = "json") String format) {

        if ("csv".equalsIgnoreCase(format)) {
            String csv = datasetPreparationService.buildCsv();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dataset_mappings.csv")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv);
        }

        List<DatasetDocument> docs = datasetPreparationService.buildDataset();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(docs);
    }
}
