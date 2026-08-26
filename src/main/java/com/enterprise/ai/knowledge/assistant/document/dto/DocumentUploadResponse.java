package com.enterprise.ai.knowledge.assistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DocumentUploadResponse {

    private String documentName;
    private String fileName;
    private String documentId;
    private Long fileSize;
    private Date uploadedAt;
    private int pages;
    private int characters;
    private int chunks;
    private int chunksCreated;
    private String text;
    private boolean isUploadSuccess;
    private java.util.List<String> chunkContents;

    // Explicit setters
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setPages(int pages) { this.pages = pages; }
    public void setCharacters(int characters) { this.characters = characters; }
    public void setChunks(int chunks) { this.chunks = chunks; }
    public void setChunksCreated(int chunksCreated) { this.chunksCreated = chunksCreated; }
    public void setText(String text) { this.text = text; }
    public void setUploadSuccess(boolean uploadSuccess) { this.isUploadSuccess = uploadSuccess; }
    public void setChunkContents(java.util.List<String> chunkContents) { this.chunkContents = chunkContents; }

    // Explicit getters
    public String getDocumentName() { return documentName; }
    public String getFileName() { return fileName; }
    public String getDocumentId() { return documentId; }
    public Long getFileSize() { return fileSize; }
    public Date getUploadedAt() { return uploadedAt; }
    public int getPages() { return pages; }
    public int getCharacters() { return characters; }
    public int getChunks() { return chunks; }
    public int getChunksCreated() { return chunksCreated; }
    public String getText() { return text; }
    public boolean isUploadSuccess() { return isUploadSuccess; }
    public java.util.List<String> getChunkContents() { return chunkContents; }
}


