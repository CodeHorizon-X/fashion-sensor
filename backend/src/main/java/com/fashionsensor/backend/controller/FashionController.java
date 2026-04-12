package com.fashionsensor.backend.controller;

import com.fashionsensor.backend.model.SuggestionResponse;
import com.fashionsensor.backend.service.OutfitSuggestionService;
import com.fashionsensor.backend.service.ExploreService;
import com.fashionsensor.backend.service.UnsplashProxyService; // Added this critical import
import org.springframework.web.bind.annotation.CrossOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

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

    // Constructor Injection
    public FashionController(OutfitSuggestionService outfitSuggestionService,
            ExploreService exploreService,
            UnsplashProxyService unsplashProxyService) {
        this.outfitSuggestionService = outfitSuggestionService;
        this.exploreService = exploreService;
        this.unsplashProxyService = unsplashProxyService;
    }

    @PostMapping(value = "/suggest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> suggest(
            @RequestParam Map<String, String> data,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            logger.info(
                    "Received outfit suggestion request. audience={}, style={}, photoPresent={}",
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

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/explore")
    public ResponseEntity<List<Map<String, String>>> explore(
            @RequestParam(required = false, defaultValue = "men") String audience,
            @RequestParam(required = false, defaultValue = "all") String style) {
        logger.info("Received explore request. audience={}, style={}", audience, style);
        return ResponseEntity.ok(exploreService.fetchExploreItems(audience, style));
    }

    @GetMapping("/unsplash")
    public Mono<ResponseEntity<String>> searchUnsplash(
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int per_page,
            @RequestParam(defaultValue = "portrait") String orientation) {
        return unsplashProxyService.searchPhotos(query, per_page, orientation)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private String getOrDefault(Map<String, String> data, String key, String fallback) {
        if (data == null)
            return fallback;
        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}