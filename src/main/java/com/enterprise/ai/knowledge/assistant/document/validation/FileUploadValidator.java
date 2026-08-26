package com.enterprise.ai.knowledge.assistant.document.validation;

import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

/**
 * Validator for file uploads to ensure security and validity
 */
public class FileUploadValidator {

    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "txt", "docx"
    );

    // Allowed MIME types
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    // Maximum file size (50MB)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * Validate a file upload
     * @throws IllegalArgumentException if validation fails
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %d bytes", MAX_FILE_SIZE)
            );
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException(
                    String.format("File extension '%s' is not allowed. Allowed extensions: %s",
                            fileExtension, ALLOWED_EXTENSIONS)
            );
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    String.format("File content type '%s' is not allowed. Allowed types: %s",
                            contentType, ALLOWED_MIME_TYPES)
            );
        }

        // Check for potential path traversal attacks
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name: potential path traversal attack");
        }
    }

    /**
     * Extract file extension from filename
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Sanitize filename to prevent path traversal attacks
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "upload";
        }
        // Remove path separators and parent directory references
        return filename.replaceAll("[./\\\\]", "_");
    }
}
