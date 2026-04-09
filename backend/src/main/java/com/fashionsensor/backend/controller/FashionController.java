package com.fashionsensor.backend.controller;

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
    "http://localhost:3000"
})
public class FashionController {

    private static final Logger logger = LoggerFactory.getLogger(FashionController.class);

    @PostMapping(
            value = "/suggest",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> suggest(
            @RequestParam Map<String, String> data,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        try {
            String style = getOrDefault(data, "style", "minimal");
            String purpose = getOrDefault(data, "purpose", "casual");
            String location = getOrDefault(data, "location", "your location");
            String withWhom = getOrDefault(data, "withWhom", "your group");
            String notes = getOrDefault(data, "notes", "");

            logger.info(
                    "Received outfit suggestion request. style={}, purpose={}, location={}, withWhom={}, photoPresent={}",
                    style,
                    purpose,
                    location,
                    withWhom,
                    photo != null && !photo.isEmpty()
            );

            Map<String, String> response = new LinkedHashMap<>();
            response.put("top", buildTopSuggestion(style, purpose));
            response.put("bottom", buildBottomSuggestion(style, purpose));
            response.put("footwear", buildFootwearSuggestion(style, purpose));
            response.put("accessories", buildAccessoriesSuggestion(style, notes));
            response.put("style", style);
            response.put("purpose", purpose);

            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            logger.error("Failed to build outfit suggestion response", exception);

            Map<String, String> errorResponse = new LinkedHashMap<>();
            errorResponse.put("top", "");
            errorResponse.put("bottom", "");
            errorResponse.put("footwear", "");
            errorResponse.put("accessories", "");
            errorResponse.put("style", getOrDefault(data, "style", "minimal"));
            errorResponse.put("purpose", getOrDefault(data, "purpose", "casual"));
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

    private String buildTopSuggestion(String style, String purpose) {
        if ("formal".equalsIgnoreCase(purpose) || "formal".equalsIgnoreCase(style)) {
            return "Structured ivory shirt";
        }
        if ("party".equalsIgnoreCase(purpose)) {
            return "Statement black satin top";
        }
        if ("athleisure".equalsIgnoreCase(style)) {
            return "Performance zip jacket";
        }
        return "Oversized white tee";
    }

    private String buildBottomSuggestion(String style, String purpose) {
        if ("formal".equalsIgnoreCase(purpose) || "formal".equalsIgnoreCase(style)) {
            return "Tailored charcoal trousers";
        }
        if ("party".equalsIgnoreCase(purpose)) {
            return "Slim dark-fit pants";
        }
        if ("travel".equalsIgnoreCase(purpose)) {
            return "Relaxed utility pants";
        }
        return "Black cargo pants";
    }

    private String buildFootwearSuggestion(String style, String purpose) {
        if ("formal".equalsIgnoreCase(purpose) || "formal".equalsIgnoreCase(style)) {
            return "Leather derby shoes";
        }
        if ("party".equalsIgnoreCase(purpose)) {
            return "Monochrome high-top sneakers";
        }
        if ("wedding".equalsIgnoreCase(purpose)) {
            return "Polished loafers";
        }
        return "Chunky white sneakers";
    }

    private String buildAccessoriesSuggestion(String style, String notes) {
        if (notes.toLowerCase().contains("neutral")) {
            return "Minimal silver watch";
        }
        if ("genz".equalsIgnoreCase(style)) {
            return "Crossbody bag and chain";
        }
        if ("formal".equalsIgnoreCase(style)) {
            return "Classic watch and leather belt";
        }
        return "Watch and layered chain";
    }
}
