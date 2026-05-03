package com.fashionsensor.backend.controller;

import com.fashionsensor.backend.model.SuggestionRequest;
import com.fashionsensor.backend.model.SuggestionResponse;
import com.fashionsensor.backend.service.ExploreService;
import com.fashionsensor.backend.service.GeminiService;
import com.fashionsensor.backend.service.OutfitSuggestionService;
import com.fashionsensor.backend.service.UnsplashProxyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
        "http://127.0.0.1:3000",
        "http://localhost:3000",
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class FashionController {

    private static final Logger logger = LoggerFactory.getLogger(FashionController.class);

    private final OutfitSuggestionService outfitSuggestionService;
    private final ExploreService exploreService;
    private final UnsplashProxyService unsplashProxyService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public FashionController(
            OutfitSuggestionService outfitSuggestionService,
            ExploreService exploreService,
            UnsplashProxyService unsplashProxyService,
            GeminiService geminiService,
            ObjectMapper objectMapper) {
        this.outfitSuggestionService = outfitSuggestionService;
        this.exploreService = exploreService;
        this.unsplashProxyService = unsplashProxyService;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    // ─── Standard Suggest ────────────────────────────────────────────────────

    @PostMapping(value = "/suggest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> suggest(
            @RequestParam Map<String, String> data,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            logger.info("Received outfit suggestion request. audience={}, style={}, photoPresent={}",
                    getOrDefault(data, "audience", "men"),
                    getOrDefault(data, "style", "casual"),
                    photo != null && !photo.isEmpty());

            SuggestionResponse response = outfitSuggestionService.generateSuggestion(data, photo);
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            logger.error("Failed to build outfit suggestion response", exception);

            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("outfit", "");
            errorResponse.put("outfits", List.of());
            errorResponse.put("items", List.of());
            errorResponse.put("style", getOrDefault(data, "style", "casual"));
            errorResponse.put("amazonLinks", Map.of());
            errorResponse.put("audience", getOrDefault(data, "audience", "men"));
            errorResponse.put("error", "Unable to generate suggestion. Please try again.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ─── Explore ─────────────────────────────────────────────────────────────

    @GetMapping("/explore")
    public ResponseEntity<List<Map<String, String>>> explore(
            @RequestParam(required = false, defaultValue = "men") String audience,
            @RequestParam(required = false, defaultValue = "all") String style) {
        logger.info("Received explore request. audience={}, style={}", audience, style);
        return ResponseEntity.ok(exploreService.fetchExploreItems(audience, style));
    }

    // ─── Unsplash Proxy ───────────────────────────────────────────────────────

    @GetMapping("/unsplash")
    public Mono<ResponseEntity<String>> searchUnsplash(
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int per_page,
            @RequestParam(defaultValue = "portrait") String orientation) {
        return unsplashProxyService.searchPhotos(query, per_page, orientation)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // ─── Agentic Suggest (Gemini) ─────────────────────────────────────────────

    /**
     * Agentic suggestion endpoint.
     * 1. Builds a chain-of-thought prompt via generateAgenticPrompt (includes notes + history).
     * 2. Sends it to Google Gemini 2.0 Flash (preferred over OpenAI per requirements).
     * 3. Parses the JSON response for { "reasoning": "...", "options": [...] }.
     * 4. Falls back gracefully if Gemini key is absent or call fails.
     */
    @PostMapping(value = "/agentic-suggest",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> agenticSuggest(@RequestBody SuggestionRequest request) {
        try {
            logger.info("Received agentic suggestion request. gender={}, purpose={}, notes='{}', history.size={}",
                    request.gender(), request.purpose(), request.notes(),
                    request.history() == null ? 0 : request.history().size());

            // Build the full agentic prompt (includes notes and history dedup logic)
            String prompt = outfitSuggestionService.generateAgenticPrompt(request);

            // ── Try Gemini first ──────────────────────────────────────────────
            if (geminiService.isAvailable()) {
                String geminiText = geminiService.generateContent(prompt);
                if (StringUtils.hasText(geminiText)) {
                    return parseAndReturnAgenticResponse(geminiText);
                }
                logger.warn("Gemini returned empty content; falling back to static response.");
            } else {
                logger.info("GEMINI_API_KEY not configured. Returning static fallback.");
            }

            // ── Static fallback ───────────────────────────────────────────────
            return buildFallbackAgenticResponse(request);

        } catch (Exception e) {
            logger.error("FATAL GEMINI EXCEPTION: ", e);
            logger.error("Exception message: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Agentic suggestion failed: " + e.getMessage()));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Parses a raw JSON string (from Gemini) for "reasoning" and "options" fields.
     */
    private ResponseEntity<?> parseAndReturnAgenticResponse(String rawJson) {
        try {
            JsonNode parsed = objectMapper.readTree(stripCodeFences(rawJson));
            String reasoning = parsed.path("reasoning").asText("No reasoning provided.");
            List<String> options = new ArrayList<>();
            parsed.path("options").forEach(node -> options.add(node.asText()));

            if (options.isEmpty()) {
                logger.warn("Gemini response had no 'options' field. raw={}", rawJson);
                // Still return reasoning even if options are empty
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("reasoning", reasoning);
            result.put("options", options);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("FATAL GEMINI EXCEPTION: Failed to parse Gemini agentic response. raw={}", rawJson, e);
            e.printStackTrace();
            return buildFallbackAgenticResponse(null);
        }
    }

    /**
     * Returns a 503 error response when Gemini is unavailable or fails.
     * The frontend handles this gracefully via Promise.allSettled — it keeps
     * the standard suggest outfits and simply hides the agent-reasoning panel.
     */
    private ResponseEntity<?> buildFallbackAgenticResponse(SuggestionRequest request) {
        logger.error("Gemini API Error: key not configured or API call failed for request gender={}, purpose={}",
                request != null ? request.gender() : "unknown",
                request != null ? request.purpose() : "unknown");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Gemini AI is currently unavailable. Please check your API key and model configuration."));
    }

    private String stripCodeFences(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "").replaceFirst("^```", "").replaceFirst("```$", "").trim();
        }
        return trimmed;
    }

    private String getOrDefault(Map<String, String> data, String key, String fallback) {
        if (data == null) return fallback;
        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}