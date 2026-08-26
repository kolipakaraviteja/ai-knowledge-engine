package com.enterprise.ai.knowledge.assistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 3.0 Configuration for Enterprise AI Knowledge Assistant.
 *
 * Provides comprehensive API documentation with:
 * - Project metadata (title, description, version)
 * - Contact and license information
 * - Server configuration (local, production)
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 * Access OpenAPI YAML at: http://localhost:8080/v3/api-docs.yaml
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.url:}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        String defaultServerUrl = serverUrl != null && !serverUrl.isEmpty() 
            ? serverUrl 
            : "http://localhost:8080";
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise AI Knowledge Assistant API")
                        .version("1.0.0")
                        .description(
                                "A comprehensive RAG (Retrieval-Augmented Generation) powered knowledge assistant " +
                                "with document management, conversation history, and context-aware chat.\n\n" +
                                "Features:\n" +
                                "- **RAG Chat**: Ask questions based on uploaded documents\n" +
                                "- **Document Management**: Upload, index, and manage knowledge base documents (PDF, TXT, DOCX)\n" +
                                "- **Conversation History**: Track and manage multi-turn conversations\n" +
                                "- **Web UI**: Full Thymeleaf + HTMX interface at `/ui/`\n" +
                                "- **Embeddings**: Vector embeddings via Spring AI with pgvector storage\n" +
                                "- **LLM Integration**: Supports OpenAI and LM Studio\n\n" +
                                "Endpoints are divided into:\n" +
                                "- **/api/chat/** - Chat endpoints (simple & RAG-enhanced)\n" +
                                "- **/api/documents/** - Document management\n" +
                                "- **/api/ui/** - HTMX UI API endpoints\n" +
                                "- **/ui/** - Web UI pages (Thymeleaf templates)"
                        )
                        .contact(new Contact()
                                .name("Enterprise AI Team")
                                .url("https://github.com/your-org/enterprise-ai-knowledge-assistant")
                                .email("support@enterprise-ai.example.com")
                        )
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")
                        )
                )
                .addServersItem(new Server()
                        .url(defaultServerUrl)
                        .description("Default Server")
                );
    }
}

