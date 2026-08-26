package com.enterprise.ai.knowledge.assistant;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@Slf4j
@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private EmbeddingService embeddingService;

	@Test
	void contextLoads() {
	}

	@Test
	//@Disabled("Requires LM Studio running with a loaded model. Run manually when LM Studio is available.")
	void testDummy() {
		String text = "Employees receive 20 days of paid time off.";

		EmbeddingResult embedding = embeddingService.generateEmbedding(text);
		float[] vector = embedding.vector();
		log.debug("Vector size : {}", vector == null ? 0 : vector.length);
		log.debug("Vector sample: {}", Arrays.toString(Arrays.copyOf(vector, Math.min(10, vector == null ? 0 : vector.length))));
	}

}
