package com.enterprise.ai.knowledge.assistant.document.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentChunkServiceTest {

    private final DocumentChunkService service = new DocumentChunkService();

    @Test
    public void testChunkSmallTextLessThanChunkSize() {
        String text = "short text";
        List<String> chunks = service.chunkText(text, 1000, 200);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    public void testChunkExactChunkSize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append('a'); // 100 chars
        String text = sb.toString();
        List<String> chunks = service.chunkText(text, 100, 0);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    public void testChunkWithOverlap() {
        String text = "abcdefghijklmnopqrstuvwxyz"; // 26 chars
        List<String> chunks = service.chunkText(text, 10, 2);
        // Expected chunks: [0-10), [8-18), [16-26) => 3 chunks
        assertEquals(3, chunks.size());
        assertEquals("abcdefghij", chunks.get(0));
        assertEquals("ijklmnopqr", chunks.get(1));
        assertEquals("qrstuvwxyz", chunks.get(2));
    }

    @Test
    public void testNegativeOverlapTreatedAsZero() {
        String text = "0123456789"; // 10 chars
        List<String> chunks = service.chunkText(text, 4, -5);
        // chunkSize 4, overlap negative -> treated as 0 => chunks: [0-4)=4,[4-8)=4,[8-10)=2 => 3 chunks
        assertEquals(3, chunks.size());
        assertEquals("0123", chunks.get(0));
        assertEquals("4567", chunks.get(1));
        assertEquals("89", chunks.get(2));
    }

    @Test
    public void testInvalidChunkSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.chunkText("hello", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.chunkText("hello", -10, 0));
    }
}

