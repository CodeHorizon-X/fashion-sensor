package com.fashionsensor.backend.controller;

import com.fashionsensor.backend.model.SuggestionRequest;
import com.fashionsensor.backend.model.SuggestionResponse;
import com.fashionsensor.backend.service.ExploreService;
import com.fashionsensor.backend.service.OutfitSuggestionService;
import com.fashionsensor.backend.service.UnsplashProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

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

    public FashionController(
            OutfitSuggestionService outfitSuggestionService,
            ExploreService exploreService,
            UnsplashProxyService unsplashProxyService) {
        this.outfitSuggestionService = outfitSuggestionService;
        this.exploreService = exploreService;
        this.unsplashProxyService = unsplashProxyService;
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

    // ─── Agentic Suggest (OpenAI) ─────────────────────────────────────────────

    /**
     * Agentic suggestion endpoint.
     * Routes directly to OutfitSuggestionService (OpenAI wrapper) — Gemini removed.
     */
    @PostMapping(value = "/agentic-suggest",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> agenticSuggest(@RequestBody SuggestionRequest request) {
        try {
            logger.info("Received agentic suggestion request. gender={}, purpose={}, styleVibe={}, notes='{}', history.size={}",
                    request.gender(), request.purpose(), request.styleVibe(), request.notes(),
                    request.history() == null ? 0 : request.history().size());

            Map<String, Object> result = outfitSuggestionService.generateAgenticSuggestion(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("OpenAI agentic suggestion failed — returning non-blocking fallback response", e);

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("reasoning", "");
            errorBody.put("options", List.of());
            errorBody.put("error", "AI Stylist is gathering data, please try again momentarily.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    private String getOrDefault(Map<String, String> data, String key, String fallback) {
        if (data == null) return fallback;
        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
