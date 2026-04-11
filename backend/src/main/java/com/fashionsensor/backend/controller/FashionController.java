package com.fashionsensor.backend.controller;

import com.fashionsensor.backend.model.SuggestionItem;
import com.fashionsensor.backend.model.SuggestionResponse;
import com.fashionsensor.backend.service.OutfitSuggestionService;
import com.fashionsensor.backend.service.UnsplashProxyService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

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
    private final UnsplashProxyService unsplashProxyService;

    public FashionController(OutfitSuggestionService outfitSuggestionService,
                             UnsplashProxyService unsplashProxyService) {
        this.outfitSuggestionService = outfitSuggestionService;
        this.unsplashProxyService = unsplashProxyService;
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
            errorResponse.put("itemCards", java.util.List.of());
            errorResponse.put("style", getOrDefault(data, "style", "casual"));
            errorResponse.put("amazonLinks", Map.of());
            errorResponse.put("pinterestQuery", "");
            errorResponse.put("audience", getOrDefault(data, "audience", "men"));
            errorResponse.put("source", "error");
            errorResponse.put("error", "Unable to generate suggestion right now. Please try again.");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * GET /api/suggestions?audience=men&style=casual
     * Returns Unsplash image cards for the Explore grid, driven by audience + style.
     */
    @GetMapping(
            value = "/suggestions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> suggestions(
            @RequestParam(value = "audience", defaultValue = "men") String audience,
            @RequestParam(value = "style", defaultValue = "casual") String style
    ) {
        try {
            logger.info("Explore suggestions request. audience={}, style={}", audience, style);

            // Build representative item titles for the chosen audience + style
            List<String> sampleTitles = buildSampleTitles(audience, style);
            List<SuggestionItem> items = unsplashProxyService.search(audience, style, sampleTitles);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("audience", audience);
            body.put("style", style);
            body.put("items", items);
            return ResponseEntity.ok(body);
        } catch (Exception exception) {
            logger.error("Failed to fetch explore suggestions", exception);
            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("error", "Unable to fetch suggestions right now.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /** Returns 4-5 representative clothing item titles for the given audience + style. */
    private List<String> buildSampleTitles(String audience, String style) {
        String normalized = (style == null ? "casual" : style.trim().toLowerCase());
        String aud = (audience == null ? "men" : audience.trim().toLowerCase());
        return switch (aud) {
            case "women" -> switch (normalized) {
                case "formal"     -> List.of("tailored blazer", "silk blouse", "pointed heels", "structured tote");
                case "genz"       -> List.of("cropped graphic tee", "wide-leg jeans", "platform sneakers", "mini shoulder bag");
                case "minimal"    -> List.of("ribbed knit top", "cream trousers", "loafers", "soft blazer");
                case "athleisure" -> List.of("performance tee", "leggings", "running shoes", "light jacket");
                default           -> List.of("neutral top", "blue jeans", "white sneakers", "cropped cardigan");
            };
            case "kids" -> switch (normalized) {
                case "formal"     -> List.of("neat shirt", "tailored chinos", "polished sneakers", "soft cardigan");
                case "genz"       -> List.of("oversized hoodie", "joggers", "chunky trainers", "crossbody pouch");
                case "minimal"    -> List.of("solid tee", "easy trousers", "slip-on shoes", "overshirt");
                case "athleisure" -> List.of("sport tee", "track pants", "running shoes", "zip hoodie");
                default           -> List.of("graphic sweatshirt", "soft denim", "play sneakers", "light cap");
            };
            default -> switch (normalized) {
                case "formal"     -> List.of("oxford shirt", "tailored trousers", "derby shoes", "navy blazer");
                case "genz"       -> List.of("boxy tee", "cargo pants", "retro sneakers", "crossbody bag");
                case "minimal"    -> List.of("fine knit tee", "straight trousers", "leather sneakers", "overshirt");
                case "athleisure" -> List.of("performance tee", "track pants", "running shoes", "zip jacket");
                default           -> List.of("white shirt", "blue jeans", "clean sneakers", "lightweight overshirt");
            };
        };
    }

    private String getOrDefault(Map<String, String> data, String key, String fallback) {
        if (data == null) {
            return fallback;
        }

        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
