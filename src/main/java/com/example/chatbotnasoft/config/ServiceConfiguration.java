package com.example.chatbotnasoft.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ServiceConfiguration {

    public ServiceConfiguration() {
        log.info("🔧 Configuration des services terminée");
    }
}
