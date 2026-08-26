package com.enterprise.ai.knowledge.assistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AIKnowledgeAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(AIKnowledgeAssistantApplication.class, args);
		log.info("Enterprise AI Knowledge Assistant application started successfully");
	}

}
