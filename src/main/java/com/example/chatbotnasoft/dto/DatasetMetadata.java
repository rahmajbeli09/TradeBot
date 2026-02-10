package com.example.chatbotnasoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetMetadata {

    private String status;

    private int version;

    private String msgType;

    private String category;

    private String agent;
}
