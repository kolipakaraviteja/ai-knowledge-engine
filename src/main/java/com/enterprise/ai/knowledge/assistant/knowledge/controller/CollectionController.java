package com.enterprise.ai.knowledge.assistant.knowledge.controller;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.Collection;
import com.enterprise.ai.knowledge.assistant.knowledge.service.CollectionService;
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
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collection API", description = "Endpoints for managing collections within knowledge bases")
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    @Operation(summary = "Create Collection", description = "Create a new collection within a knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Collection> createCollection(
            @Parameter(description = "Knowledge base ID", required = true)
            @RequestParam String knowledgeBaseId,
            @Parameter(description = "Collection name", required = true)
            @RequestParam String name,
            @Parameter(description = "Collection description")
            @RequestParam(required = false) String description,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        log.info("Creating collection: {} in knowledge base: {} for user: {}", name, knowledgeBaseId, userId);
        Collection coll = collectionService.createCollection(UUID.fromString(knowledgeBaseId), name, description);
        return ResponseEntity.ok(coll);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Collection", description = "Get a collection by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<Collection> getCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        return collectionService.getCollection(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List Collections", description = "Get all collections for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of collections"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<Collection>> getAllCollections(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        List<Collection> collections = collectionService.getAllCollections();
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    @Operation(summary = "List Collections by Knowledge Base", description = "Get all collections for a specific knowledge base")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of collections"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<Collection>> getCollectionsByKnowledgeBase(
            @Parameter(description = "Knowledge base ID", required = true)
            @PathVariable UUID knowledgeBaseId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        List<Collection> collections = collectionService.getCollectionsByKnowledgeBase(knowledgeBaseId);
        return ResponseEntity.ok(collections);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Collection", description = "Update a collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection updated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<Collection> updateCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Collection name", required = true)
            @RequestParam String name,
            @Parameter(description = "Collection description")
            @RequestParam(required = false) String description,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        try {
            Collection coll = collectionService.updateCollection(id, name, description);
            return ResponseEntity.ok(coll);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Collection", description = "Delete a collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<Void> deleteCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID userId) {
        collectionService.deleteCollection(id);
        return ResponseEntity.ok().build();
    }
}
