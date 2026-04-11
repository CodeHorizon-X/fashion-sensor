package com.fashionsensor.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashionsensor.backend.model.SuggestionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class OutfitSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(OutfitSuggestionService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiModel;
    private final Random random = new Random();

    public OutfitSuggestionService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api.key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4o-mini}") String openAiModel
    ) {
        this.webClient = webClientBuilder.baseUrl(OPENAI_API_URL).build();
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
    }

    public SuggestionResponse generateSuggestion(Map<String, String> requestData, MultipartFile photo) {
        Map<String, String> normalized = normalize(requestData);
        String audience = normalized.get("audience");

        if (photo != null && !photo.isEmpty()) {
            try {
                SuggestionResponse aiSuggestion = generateImageSuggestion(normalized, photo);
                if (aiSuggestion != null) {
                    return aiSuggestion;
                }
            } catch (Exception exception) {
                logger.warn("Image analysis failed. Falling back to rules-based recommendation.", exception);
            }
        }

        return generateFallbackSuggestion(normalized, photo != null && !photo.isEmpty(), audience);
    }

    private SuggestionResponse generateImageSuggestion(Map<String, String> requestData, MultipartFile photo) throws IOException {
        if (!StringUtils.hasText(openAiApiKey)) {
            logger.info("OPENAI_API_KEY is not configured. Skipping image analysis.");
            return null;
        }

        String audience = requestData.get("audience");
        String style = requestData.get("style");
        String purpose = requestData.get("purpose");
        String location = requestData.get("location");

        String base64 = Base64.getEncoder().encodeToString(photo.getBytes());
        String mimeType = StringUtils.hasText(photo.getContentType()) ? photo.getContentType() : MediaType.IMAGE_JPEG_VALUE;

        Map<String, Object> payload = buildVisionPayload(requestData, base64, mimeType);

        String rawResponse = webClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (!StringUtils.hasText(rawResponse)) {
            return null;
        }

        JsonNode root = objectMapper.readTree(rawResponse);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            logger.warn("OpenAI response did not contain content. response={}", rawResponse);
            return null;
        }

        JsonNode parsed = objectMapper.readTree(stripCodeFences(content));
        List<String> items = sanitizeItems(parsed.path("items"));
        if (items.isEmpty()) {
            items = buildFallbackItems(style, purpose, audience, location);
        }

        String resolvedStyle = defaultIfBlank(parsed.path("style").asText(""), style);
        List<String> outfits = sanitizeItems(parsed.path("outfits"));
        if (outfits.isEmpty()) {
            outfits = buildFallbackOutfits(defaultIfBlank(resolvedStyle, style), purpose, audience, location);
        }
        String outfit = defaultIfBlank(parsed.path("outfit").asText(""), outfits.get(0));
        String pinterestQuery = buildPinterestQuery(defaultIfBlank(resolvedStyle, style), audience);

        return new SuggestionResponse(
                outfit,
                outfits,
                items,
                defaultIfBlank(resolvedStyle, "casual"),
                buildAmazonLinks(items, audience),
                pinterestQuery,
                audience,
                "ai-image"
        );
    }

    private Map<String, Object> buildVisionPayload(Map<String, String> requestData, String base64, String mimeType) {
        String prompt = """
                Analyze the uploaded outfit or wardrobe image and respond with strict JSON only.
                Required JSON shape:
                {
                  "outfits": ["look 1", "look 2", "look 3"],
                  "items": ["item 1", "item 2", "item 3", "item 4"],
                  "outfit": "full outfit recommendation",
                  "style": "style keyword"
                }
                Provide 3 distinct outfit options and at least 4 concrete items. Keep the style concise.
                Infer clothing pieces, colors, and overall vibe from the image.
                Use this context when relevant:
                audience=%s
                purpose=%s
                location=%s
                withWhom=%s
                notes=%s
                """.formatted(
                requestData.get("audience"),
                requestData.get("purpose"),
                requestData.get("location"),
                requestData.get("withWhom"),
                requestData.get("notes")
        );

        Map<String, Object> imageUrl = Map.of(
                "url", "data:" + mimeType + ";base64," + base64
        );

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", prompt
        );

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", imageUrl
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent)
        );

        String baseSystemContent = "You are a fashion stylist and vision assistant. Return valid JSON only.";
        String userNotes = requestData.get("notes");
        if (StringUtils.hasText(userNotes)) {
            baseSystemContent += " CRITICAL CONSTRAINT: The user explicitly noted: '" + userNotes + "'. You MUST STRICTLY exclude any items, colors, or styles mentioned as a negative (e.g. 'no white', 'avoid red', 'without hat'). DO NOT include them in any response.";
        }

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", baseSystemContent
        );

        Map<String, Object> responseFormat = Map.of(
                "type", "json_object"
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("temperature", 0.3);
        payload.put("response_format", responseFormat);
        payload.put("messages", List.of(systemMessage, userMessage));
        return payload;
    }

    private SuggestionResponse generateFallbackSuggestion(Map<String, String> requestData, boolean imageAttempted, String audience) {
        String style = requestData.get("style");
        String purpose = requestData.get("purpose");
        String location = requestData.get("location");
        List<String> items = buildFallbackItems(style, purpose, audience, location);
        List<String> outfits = buildFallbackOutfits(style, purpose, audience, location);
        String outfit = outfits.get(0);

        return new SuggestionResponse(
                outfit,
                outfits,
                items,
                style,
                buildAmazonLinks(items, audience),
                buildPinterestQuery(style, audience),
                audience,
                imageAttempted ? "fallback-after-image" : "form-fallback"
        );
    }

    private Map<String, String> normalize(Map<String, String> source) {
        Map<String, String> normalized = new LinkedHashMap<>();
        normalized.put("audience", defaultIfBlank(getValue(source, "audience"), "men"));
        normalized.put("style", defaultIfBlank(getValue(source, "style"), "casual"));
        normalized.put("purpose", defaultIfBlank(getValue(source, "purpose"), "casual"));
        normalized.put("location", defaultIfBlank(getValue(source, "location"), "city"));
        normalized.put("withWhom", defaultIfBlank(getValue(source, "withWhom"), "friends"));
        normalized.put("notes", defaultIfBlank(getValue(source, "notes"), ""));
        return normalized;
    }

    private String getValue(Map<String, String> source, String key) {
        if (source == null) {
            return "";
        }
        String value = source.get(key);
        return value == null ? "" : value.trim();
    }

    private List<String> buildFallbackItems(String style, String purpose, String audience, String location) {
        String normalizedStyle = defaultIfBlank(style, "casual").toLowerCase(Locale.ENGLISH);
        String normalizedAudience = defaultIfBlank(audience, "men").toLowerCase(Locale.ENGLISH);

        List<String> tops = normalizedAudience.contains("women") 
            ? List.of("silk blouse", "ribbed knit top", "cropped graphic tee", "tailored blazer", "neutral tank")
            : (normalizedAudience.contains("kids") 
                ? List.of("graphic sweatshirt", "solid tee", "striped polo", "hoodie", "soft cardigan") 
                : List.of("crisp oxford shirt", "boxy tee", "fine knit sweater", "navy blazer", "lightweight overshirt"));

        List<String> bottoms = normalizedAudience.contains("women")
            ? List.of("straight-fit trousers", "wide-leg jeans", "pleated midi skirt", "flare leggings", "tailored shorts")
            : (normalizedAudience.contains("kids")
                ? List.of("soft denim", "cargo shorts", "joggers", "tailored chinos", "corduroy pants")
                : List.of("slim-fit trousers", "baggy jeans", "pleated chinos", "cargo pants", "tapered denim"));

        List<String> shoes = normalizedAudience.contains("women")
            ? List.of("platform sneakers", "pointed heels", "loafers", "sleek flats", "ankle boots")
            : (normalizedAudience.contains("kids")
                ? List.of("play sneakers", "slip-on shoes", "sporty sandals", "colorful trainers", "canvas shoes")
                : List.of("clean sneakers", "leather loafers", "derby shoes", "chunky trainers", "skate shoes"));

        List<String> accessories = normalizedAudience.contains("women")
            ? List.of("structured tote", "gold hoops", "mini shoulder bag", "silk scarf", "layered rings")
            : (normalizedAudience.contains("kids")
                ? List.of("light cap", "mini backpack", "colorful beanie", "fun watch", "crossbody pouch")
                : List.of("minimal watch", "leather belt", "silver chain", "crossbody bag", "polarized sunglasses"));

        return List.of(
            tops.get(random.nextInt(tops.size())),
            bottoms.get(random.nextInt(bottoms.size())),
            shoes.get(random.nextInt(shoes.size())),
            accessories.get(random.nextInt(accessories.size())),
            "signature fragrance"
        );
    }

    private List<String> buildFallbackOutfits(String style, String purpose, String audience, String location) {
        List<String> outfits = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<String> items = buildFallbackItems(style, purpose, audience, location);
            outfits.add(items.get(0) + " + " + items.get(1) + " + " + items.get(2) + " + " + items.get(3));
        }
        return outfits;
    }

    private List<String> sanitizeItems(JsonNode itemsNode) {
        if (!itemsNode.isArray()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            String value = itemNode.asText("").trim();
            if (StringUtils.hasText(value)) {
                items.add(value);
            }
        }
        return items;
    }

    private Map<String, String> buildAmazonLinks(List<String> items, String audience) {
        Map<String, String> links = new LinkedHashMap<>();
        for (String item : items) {
            String key = buildItemKey(item);
            String query = item + " " + defaultIfBlank(audience, "fashion");
            links.put(key, "https://www.amazon.in/s?k=" + urlEncode(query));
        }
        return links;
    }

    private String buildPinterestQuery(String style, String audience) {
        return (defaultIfBlank(style, "casual") + " " + defaultIfBlank(audience, "fashion") + " outfit")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildItemKey(String item) {
        String[] words = item.toLowerCase(Locale.ENGLISH).split("\\s+");
        return words.length == 0 ? "item" : words[words.length - 1].replaceAll("[^a-z]", "");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String stripCodeFences(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "").replaceFirst("^```", "").replaceFirst("```$", "").trim();
        }
        return trimmed;
    }
}
