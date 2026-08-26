package com.enterprise.ai.knowledge.assistant.document.dto;

/**
 * Parsed document payload returned by file parsers.
 */
public record ParsedDocument(
        String text,
        int pageCount,
        boolean pageAware,
        String mimeType
) {
}

