package com.enterprise.ai.knowledge.assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${web.url:}")
    private String webUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins;
        
        if (webUrl != null && !webUrl.isEmpty()) {
            // Use the configured web URL from environment variable
            allowedOrigins = new String[]{webUrl};
        } else {
            // Fallback to localhost origins for local development
            allowedOrigins = new String[]{
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:4200"
            };
        }
        
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
