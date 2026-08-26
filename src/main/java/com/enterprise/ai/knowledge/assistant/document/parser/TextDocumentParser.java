package com.enterprise.ai.knowledge.assistant.document.parser;

import com.enterprise.ai.knowledge.assistant.document.dto.ParsedDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TextDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".html") || lower.endsWith(".htm");
    }

    @Override
    public ParsedDocument parse(Path filePath) throws IOException {
        String text = Files.readString(filePath, StandardCharsets.UTF_8);
        return new ParsedDocument(text, 1, false, guessMime(filePath));
    }

    private String guessMime(Path filePath) {
        String name = filePath == null ? "" : filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".md") || name.endsWith(".markdown")) return "text/markdown";
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        return "text/plain";
    }
}

