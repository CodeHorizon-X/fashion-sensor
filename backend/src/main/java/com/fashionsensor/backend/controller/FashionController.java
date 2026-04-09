package com.fashionsensor.backend.controller;

import com.fashionsensor.backend.model.SuggestionResponse;
import com.fashionsensor.backend.service.OutfitSuggestionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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

    public FashionController(OutfitSuggestionService outfitSuggestionService) {
        this.outfitSuggestionService = outfitSuggestionService;
    }

    @PostMapping(
            value = "/suggest",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> suggest(
            @RequestParam Map<String, String> data,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        try {
            logger.info(
                    "Received outfit suggestion request. audience={}, style={}, purpose={}, location={}, withWhom={}, photoPresent={}",
                    getOrDefault(data, "audience", "men"),
                    getOrDefault(data, "style", "casual"),
                    getOrDefault(data, "purpose", "casual"),
                    getOrDefault(data, "location", "city"),
                    getOrDefault(data, "withWhom", "friends"),
                    photo != null && !photo.isEmpty()
            );

            SuggestionResponse response = outfitSuggestionService.generateSuggestion(data, photo);
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            logger.error("Failed to build outfit suggestion response", exception);

            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("outfit", "");
            errorResponse.put("outfits", java.util.List.of());
            errorResponse.put("items", java.util.List.of());
            errorResponse.put("style", getOrDefault(data, "style", "casual"));
            errorResponse.put("amazonLinks", Map.of());
            errorResponse.put("pinterestQuery", "");
            errorResponse.put("audience", getOrDefault(data, "audience", "men"));
            errorResponse.put("source", "error");
            errorResponse.put("error", "Unable to generate suggestion right now. Please try again.");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private String getOrDefault(Map<String, String> data, String key, String fallback) {
        if (data == null) {
            return fallback;
        }

        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
