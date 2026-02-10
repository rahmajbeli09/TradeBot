package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetDocument {

    private String id;

    private String msgType;

    private String text;

    private DatasetMetadata metadata;
}
