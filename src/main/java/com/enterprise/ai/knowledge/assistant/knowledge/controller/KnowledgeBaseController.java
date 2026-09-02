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
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<KnowledgeBase> createKnowledgeBase(
            @Parameter(description = "Knowledge base name", required = true)
            @RequestParam String name,
            @Parameter(description = "Knowledge base description")
            @RequestParam(required = false) String description,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        log.info("Creating knowledge base: {} for user: {}", name, userId);
        KnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(name, description, userId);
        return ResponseEntity.ok(kb);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Knowledge Base", description = "Get a knowledge base by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base found"),
            @ApiResponse(responseCode = "404", description = "Knowledge base not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<KnowledgeBase> getKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        log.info("Fetching knowledge base with id: {} for user: {}", id, userId);
        return knowledgeBaseService.getKnowledgeBase(id, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Knowledge base not found or access denied with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping
    @Operation(summary = "List Knowledge Bases", description = "Get all knowledge bases for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of knowledge bases"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<KnowledgeBase>> getAllKnowledgeBases(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        log.info("Fetching knowledge bases for user: {}", userId);
        List<KnowledgeBase> bases = knowledgeBaseService.getAllKnowledgeBases(userId);
        log.info("Retrieved {} knowledge base(s) for user: {}", bases.size(), userId);
        return ResponseEntity.ok(bases);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Knowledge Base", description = "Update a knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base updated successfully"),
            @ApiResponse(responseCode = "404", description = "Knowledge base not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<KnowledgeBase> updateKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Knowledge base name", required = true)
            @RequestParam String name,
            @Parameter(description = "Knowledge base description")
            @RequestParam(required = false) String description,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        log.info("Updating knowledge base with id: {} for user: {}", id, userId);
        try {
            KnowledgeBase kb = knowledgeBaseService.updateKnowledgeBase(id, name, description, userId);
            log.info("Knowledge base updated successfully with id: {}", id);
            return ResponseEntity.ok(kb);
        } catch (IllegalArgumentException e) {
            log.warn("Knowledge base not found or access denied for update with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Knowledge Base", description = "Delete a knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Knowledge base not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        log.info("Deleting knowledge base with id: {} for user: {}", id, userId);
        try {
            knowledgeBaseService.deleteKnowledgeBase(id, userId);
            log.info("Knowledge base deleted successfully with id: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Knowledge base not found or access denied for delete with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
