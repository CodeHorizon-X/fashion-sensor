package com.fashionsensor.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * GeminiService routes fashion outfit prompts directly to the Google Gemini
 * Generative Language REST API using the stable v1beta endpoint.
 *
 * <p>Model is locked to {@value #GEMINI_MODEL} for production cluster routing.
 * The API key is resolved at startup from the {@code GEMINI_API_KEY} environment
 * variable (or the {@code gemini.api.key} Spring property) — no custom base URL
 * or endpoint override is needed.
 */
@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    /** Production model identifier — do NOT change without a full deployment review. */
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GeminiService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key:}") String apiKey) {

        // Build the client without custom endpoints or base-URL overrides so
        // it always resolves against the live production cluster.
        this.webClient = webClientBuilder
                .baseUrl(GEMINI_BASE_URL)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    /**
     * Sends {@code prompt} to Gemini and returns the first text candidate.
     *
     * @param prompt natural-language prompt for the model
     * @return the raw text content from Gemini's first response candidate
     * @throws RuntimeException if the API call fails or the response is empty
     */
    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured. " +
                    "Set the gemini.api.key property or the GEMINI_API_KEY environment variable.");
        }

        // Build Gemini request body
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        String endpoint = GEMINI_MODEL + ":generateContent?key=" + apiKey;

        logger.info("Sending prompt to Gemini model={} promptLength={}", GEMINI_MODEL, prompt.length());

        try {
            String rawResponse = webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new RuntimeException("Gemini returned an empty response body.");
            }

            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            logger.info("Gemini response received. textLength={}", text.length());

            if (text.isBlank()) {
                throw new RuntimeException("Gemini response contained no text content.");
            }

            return text;

        } catch (RuntimeException e) {
            logger.error("Gemini generateContent failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Gemini generateContent encountered an unexpected error", e);
            throw new RuntimeException("Gemini request failed: " + e.getMessage(), e);
        }
    }
}
