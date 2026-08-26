package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple metadata filter for SearchResult lists.
 * Default behavior is pass-through. Add criteria as needed.
 */
@Component
public class MetaDataFilter {

	/**
	 * Small criteria holder for filtering.
	 */
	public static class Criteria {
		private Double minScore;
		private Set<String> allowedDocuments;
		private Set<String> excludedDocuments;

		public Criteria() {}

		public Double getMinScore() { return minScore; }
		public void setMinScore(Double minScore) { this.minScore = minScore; }

		public Set<String> getAllowedDocuments() { return allowedDocuments; }
		public void setAllowedDocuments(Set<String> allowedDocuments) { this.allowedDocuments = allowedDocuments; }

		public Set<String> getExcludedDocuments() { return excludedDocuments; }
		public void setExcludedDocuments(Set<String> excludedDocuments) { this.excludedDocuments = excludedDocuments; }
	}

	/**
	 * Filter results using provided criteria. If criteria is null, returns input list.
	 */
	public List<SearchResult> filter(List<SearchResult> results, Criteria criteria) {
		if (results == null || results.isEmpty() || criteria == null) {
			return results;
		}

		return results.stream()
				.filter(Objects::nonNull)
				.filter(r -> {
					if (criteria.getMinScore() != null && r.getScore() < criteria.getMinScore()) {
						return false;
					}
					if (criteria.getAllowedDocuments() != null && !criteria.getAllowedDocuments().isEmpty()) {
						return criteria.getAllowedDocuments().contains(r.getDocumentName());
					}
					if (criteria.getExcludedDocuments() != null && !criteria.getExcludedDocuments().isEmpty()) {
						return !criteria.getExcludedDocuments().contains(r.getDocumentName());
					}
					return true;
				})
				.collect(Collectors.toList());
	}
}
