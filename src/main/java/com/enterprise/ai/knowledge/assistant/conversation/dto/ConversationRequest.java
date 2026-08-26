package com.enterprise.ai.knowledge.assistant.conversation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConversationRequest {
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 10000, message = "Message must be between 1 and 10000 characters")
    private String message;

    @Min(value = 1, message = "History depth must be at least 1")
    @Max(value = 20, message = "History depth cannot exceed 20")
    private int historyDepth;

    public ConversationRequest() {}

    public ConversationRequest(String message, int historyDepth) {
        this.message = message;
        this.historyDepth = historyDepth;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getHistoryDepth() { return historyDepth; }
    public void setHistoryDepth(int historyDepth) { this.historyDepth = historyDepth; }
}

