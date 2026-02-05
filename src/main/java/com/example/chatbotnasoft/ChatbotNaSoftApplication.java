package com.example.chatbotnasoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ChatbotNaSoftApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotNaSoftApplication.class, args);
    }

}
