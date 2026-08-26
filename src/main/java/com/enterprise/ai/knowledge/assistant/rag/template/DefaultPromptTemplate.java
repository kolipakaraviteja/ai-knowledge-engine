package com.enterprise.ai.knowledge.assistant.rag.template;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default RAG prompt template - comprehensive and detailed.
 * Includes full context formatting, document info, and clear instructions.
 */
@Component("defaultPromptTemplate")
public class DefaultPromptTemplate implements PromptTemplate {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            SYSTEM
            
            You are an Enterprise AI Knowledge Assistant.
            
            Rules:
            
            1. Answer ONLY using the retrieved context.
            2. Never infer missing information.
            3. If the retrieved context contains placeholders,
               unresolved values, optional text, or alternatives
               such as:
                  [can/cannot]
                  [Company Name]
                  [TBD]
                  [Optional]
                  
                  quote it exactly.
            
            4. When the source contains ambiguous wording,
               quote it exactly.
            
            5. Do not rewrite ambiguous statements.
            
            6. Never fabricate information.
            
            7. Cite the supporting document and page.
            """;

    @Override
    public String renderSystem(List<SearchResult> sources) {
        return DEFAULT_SYSTEM_PROMPT;
    }

    @Override
    public String renderUser(String query, List<SearchResult> sources) {
        StringBuilder contextBuilder = new StringBuilder();

        if (sources != null && !sources.isEmpty()) {
            contextBuilder.append("\n----------\nCONTEXT\n\n");

            for (int i = 0; i < sources.size(); i++) {
                SearchResult result = sources.get(i);
                contextBuilder.append("""
                        ====================================
                        Document : %s
                        Page     : %d
                        Chunk    : %d
                        Score    : %.3f
                        
                        Content:
                        %s
                        
                        ====================================
                        
                        """.formatted(
                        result.getDocumentName(),
                        result.getPageNumber() == null ? 0 : result.getPageNumber(),
                        result.getChunkIndex() == null ? 0 : result.getChunkIndex(),
                        result.getScore(),
                        result.getContent()
                ));
            }

            contextBuilder.append("----------\n");
        }

        contextBuilder.append("QUESTION\n\n").append(query).append("\n\n");
        contextBuilder.append("----------\nANSWER\n");

        return contextBuilder.toString();
    }

    @Override
    public String getName() {
        return "default";
    }
}

