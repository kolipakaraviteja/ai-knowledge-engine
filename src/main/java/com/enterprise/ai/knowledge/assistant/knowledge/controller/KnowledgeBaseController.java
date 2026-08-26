package com.enterprise.ai.knowledge.assistant.knowledge.controller;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.KnowledgeBase;
import com.enterprise.ai.knowledge.assistant.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Knowledge Base API", description = "Endpoints for managing knowledge bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    @Operation(summary = "Create Knowledge Base", description = "Create a new knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<KnowledgeBase> createKnowledgeBase(
            @Parameter(description = "Knowledge base name", required = true)
            @RequestParam String name,
            @Parameter(description = "Knowledge base description")
            @RequestParam(required = false) String description) {
        log.info("Creating knowledge base: {}", name);
        KnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(name, description);
        return ResponseEntity.ok(kb);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Knowledge Base", description = "Get a knowledge base by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base found"),
            @ApiResponse(responseCode = "404", description = "Knowledge base not found")
    })
    public ResponseEntity<KnowledgeBase> getKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID id) {
        log.info("Fetching knowledge base with id: {}", id);
        return knowledgeBaseService.getKnowledgeBase(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Knowledge base not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping
    @Operation(summary = "List Knowledge Bases", description = "Get all knowledge bases")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of knowledge bases")
    })
    public ResponseEntity<List<KnowledgeBase>> getAllKnowledgeBases() {
        log.info("Fetching all knowledge bases");
        List<KnowledgeBase> bases = knowledgeBaseService.getAllKnowledgeBases();
        log.info("Retrieved {} knowledge base(s)", bases.size());
        return ResponseEntity.ok(bases);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Knowledge Base", description = "Update a knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base updated successfully"),
            @ApiResponse(responseCode = "404", description = "Knowledge base not found")
    })
    public ResponseEntity<KnowledgeBase> updateKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Knowledge base name", required = true)
            @RequestParam String name,
            @Parameter(description = "Knowledge base description")
            @RequestParam(required = false) String description) {
        log.info("Updating knowledge base with id: {}", id);
        try {
            KnowledgeBase kb = knowledgeBaseService.updateKnowledgeBase(id, name, description);
            log.info("Knowledge base updated successfully with id: {}", id);
            return ResponseEntity.ok(kb);
        } catch (IllegalArgumentException e) {
            log.warn("Knowledge base not found for update with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Knowledge Base", description = "Delete a knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Knowledge base not found")
    })
    public ResponseEntity<Void> deleteKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID id) {
        log.info("Deleting knowledge base with id: {}", id);
        knowledgeBaseService.deleteKnowledgeBase(id);
        log.info("Knowledge base deleted successfully with id: {}", id);
        return ResponseEntity.ok().build();
    }
}
