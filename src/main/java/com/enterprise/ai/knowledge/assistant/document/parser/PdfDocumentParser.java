package com.enterprise.ai.knowledge.assistant.document.parser;

import com.enterprise.ai.knowledge.assistant.document.dto.ParsedDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        return lower.endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(Path filePath) throws IOException {
        try (PDDocument pdf = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            return new ParsedDocument(text == null ? "" : text, pdf.getNumberOfPages(), true, "application/pdf");
        }
    }
}

