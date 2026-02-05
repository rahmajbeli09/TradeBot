package com.example.chatbotnasoft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    
    private String apiKey;
    private String model = "gemini-1.5-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private double temperature = 0.1;
    private int maxTokens = 1024;
}
