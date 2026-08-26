package com.enterprise.ai.knowledge.assistant.document.service;

import com.enterprise.ai.knowledge.assistant.document.dto.PdfChunk;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service that splits long document text into smaller chunks.
 *
 * Strategy implemented: sliding window chunking with configurable chunk size and overlap (in characters).
 * - chunkSize: maximum characters per chunk
 * - overlap: number of characters that overlap between consecutive chunks
 */
@Slf4j
@Service
public class DocumentChunkService {

	/** Default chunk size in characters. */
	public static final int DEFAULT_CHUNK_SIZE = 1000;

	/** Default overlap in characters between chunks. */
	public static final int DEFAULT_OVERLAP = 200;

	/**
	 * Chunk text using default chunk size and overlap.
	 */
	public List<String> chunkText(String text) {
		return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
	}

	/**
	 * Chunk text using provided chunk size and overlap.
	 * Returns an empty list for null/empty input.
	 */
	public List<String> chunkText(String text, int chunkSize, int overlap) {
		if (text == null || text.isEmpty()) {
			return Collections.emptyList();
		}

		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize must be > 0");
		}

		// Ensure overlap is non-negative and less than chunkSize to make progress
		if (overlap < 0) {
			overlap = 0;
		}
		if (overlap >= chunkSize) {
			overlap = Math.max(0, chunkSize / 10);
		}

		List<String> chunks = new ArrayList<>();
		int length = text.length();
		int start = 0;

		while (start < length) {
			int end = Math.min(start + chunkSize, length);
			String chunk = text.substring(start, end);
			chunks.add(chunk);

			if (end == length) {
				break;
			}

			start = end - overlap;
			if (start < 0) {
				start = 0;
			}
		}

		return chunks;
	}

	/**
	 * Returns the number of chunks that would be produced for the given text and parameters.
	 */
	public int countChunks(String text, int chunkSize, int overlap) {
		return chunkText(text, chunkSize, overlap).size();
	}

	/** Convenience method for counting with defaults. */
	public int countChunks(String text) {
		return countChunks(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
	}

	public List<PdfChunk> chunkPDFText(
			PDDocument pdf,
			int chunkSize,
			int defaultOverlap) throws IOException {

		List<PdfChunk> chunks = new ArrayList<>();

		PDFTextStripper stripper = new PDFTextStripper();

		int chunkIndex = 0;

		for (int page = 1; page <= pdf.getNumberOfPages(); page++) {

			stripper.setStartPage(page);
			stripper.setEndPage(page);

			String text = stripper.getText(pdf);

			if (text == null || text.isBlank()) {
				continue;
			}

			text = text.replaceAll("\\s+", " ").trim();

			Pattern pattern = Pattern.compile("\\[[^\\]]+\\]");

			int start = 0;

			while (start < text.length()) {

				int end = Math.min(start + chunkSize, text.length());

				String chunk = text.substring(start, end).trim();

				chunks.add(new PdfChunk(
						page,
						chunkIndex++,
						chunk
				));

				if (end == text.length()) {
					break;
				}

				start = end - defaultOverlap;
			}
		}

		return chunks;
	}
}
