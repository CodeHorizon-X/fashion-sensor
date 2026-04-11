package com.fashionsensor.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashionsensor.backend.model.SuggestionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UnsplashProxyService {

    private static final Logger logger = LoggerFactory.getLogger(UnsplashProxyService.class);
    private static final String UNSPLASH_BASE_URL = "https://api.unsplash.com";
    private static final String FALLBACK_IMAGE = "https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=1200&q=80";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private String apiKey = "jU02FOTcxGguuEdThH4J0YJDzsAVWTlFt66zIAqtlA8";

    public UnsplashProxyService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(UNSPLASH_BASE_URL).build();
        this.objectMapper = objectMapper;
    }

    public List<SuggestionItem> search(String audience, String style, List<String> itemTitles) {
        List<String> normalizedTitles = itemTitles == null ? List.of() : itemTitles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(5)
                .toList();

        if (normalizedTitles.isEmpty()) {
            return List.of();
        }

        List<SuggestionItem> results = new ArrayList<>();
        for (String title : normalizedTitles) {
            results.add(new SuggestionItem(title, searchImageUrl(audience, style, title)));
        }
        return results;
    }

    private String searchImageUrl(String audience, String style, String title) {
        String query = buildSearchQuery(audience, style, title);

        try {
            String rawResponse = webClient.get()
                    .uri((builder) -> builder
                            .path("/search/photos")
                            .queryParam("query", query)
                            .queryParam("orientation", "portrait")
                            .queryParam("per_page", 1)
                            .queryParam("client_id", apiKey)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (!StringUtils.hasText(rawResponse)) {
                return fallbackImageUrl(title);
            }

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode firstResult = root.path("results").path(0);
            String imageUrl = firstResult.path("urls").path("regular").asText("");
            return StringUtils.hasText(imageUrl) ? imageUrl : fallbackImageUrl(title);
        } catch (Exception exception) {
            logger.warn("Unsplash image lookup failed for query={}", query, exception);
            return fallbackImageUrl(title);
        }
    }

    private String buildSearchQuery(String audience, String style, String title) {
        // Spec: audience + " " + style + " fashion outfit"
        String baseQuery = (normalizeKeyword(audience) + " " + normalizeKeyword(style) + " fashion outfit")
                .replaceAll("\\s+", " ")
                .trim();

        if (!StringUtils.hasText(title)) {
            return baseQuery;
        }

        // Append item keyword for per-card image differentiation
        return (baseQuery + " " + normalizeKeyword(title))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String fallbackImageUrl(String title) {
        String seed = normalizeKeyword(title).replace(' ', '-');
        return FALLBACK_IMAGE + "&" + seed;
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value)
                ? value.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9\\s-]", " ")
                : "";
    }
}
