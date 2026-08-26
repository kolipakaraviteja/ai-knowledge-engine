package com.enterprise.ai.knowledge.assistant.document.dto;

import lombok.Builder;

@Builder
public record PdfChunk(int pageNumber, int chunkIndex, String text) {

}