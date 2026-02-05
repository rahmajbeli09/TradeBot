package com.example.chatbotnasoft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file-watcher")
public class FileWatcherProperties {

    private String inputDirectory = "input/feeds";
    private String filePattern = "FEED*.txt";
    private int stabilizationDelayMinutes = 5;
    private int checkIntervalSeconds = 30;
    private long maxFileSizeMb = 100;

    public long getStabilizationDelayMillis() {
        return stabilizationDelayMinutes * 60 * 1000L;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024L;
    }
}
