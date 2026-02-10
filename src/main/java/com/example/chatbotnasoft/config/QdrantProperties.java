package com.example.chatbotnasoft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    private String url = "http://localhost:6333";

    private String collection = "feed_embeddings";

    private String distance = "Cosine";

    private int indexBatchSize = 32;
}
