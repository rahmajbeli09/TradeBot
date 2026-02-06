package com.example.chatbotnasoft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Configuration de sécurité pour l'application
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Permettre l'accès aux API de feed-summary sans authentification
                .requestMatchers("/api/feed-summary/**").permitAll()
                // Permettre l'accès aux API de debug
                .requestMatchers("/api/debug/**").permitAll()
                // Permettre l'accès à l'API d'anonymisation
                .requestMatchers("/api/anonymize/**").permitAll()
                // Permettre l'accès à l'API LLM
                .requestMatchers("/api/llm/**").permitAll()
                // Permettre l'accès aux mappings
                .requestMatchers("/api/feed-mappings/**").permitAll()
                // Permettre l'accès à H2 console (si utilisé)
                .requestMatchers("/h2-console/**").permitAll()
                // Permettre l'accès aux actifs statiques
                .requestMatchers("/static/**", "/css/**", "/js/**").permitAll()
                // Toutes les autres requêtes nécessitent une authentification
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {}) // Authentification Basic pour les autres endpoints
            .formLogin(form -> form
                .disable() // Désactiver le formulaire de login par défaut
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("USER", "ADMIN")
                .build();

        UserDetails testUser = User.builder()
                .username("test")
                .password(passwordEncoder.encode("test123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user, testUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
