package com.enterprise.ai.knowledge.assistant.rag.rewriter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QueryRewriter {

    private final ChatClient chatClient;

    @Value("${app.rag.enableQueryRewriting:false}")
    private boolean enableQueryRewriting;

    public QueryRewriter(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String rewrite(String query, String conversationHistory) {
        if (!enableQueryRewriting || conversationHistory == null || conversationHistory.isEmpty()) {
            return query;
        }

        try {
            String prompt = String.format("""
                Given the conversation history below, rewrite the user's latest question \
                to be standalone and unambiguous. If the question is already clear, \
                return it unchanged without any explanation.
                
                Conversation History:
                %s
                
                Latest Question: %s
                
                Rewritten Question:
                """, conversationHistory, query);

            String rewritten = chatClient.prompt()
                    .system("You are a query clarification assistant. Rewrite ambiguous questions based on context. Return only the rewritten question, nothing else.")
                    .user(prompt)
                    .call()
                    .content()
                    .trim();

            return rewritten.isEmpty() ? query : rewritten;
        } catch (Exception e) {
            return query;
        }
    }

    public boolean isEnabled() {
        return enableQueryRewriting;
    }
}

