package com.fashionsensor.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashionsensor.backend.model.SuggestionRequest;
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
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Service
public class OutfitSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(OutfitSuggestionService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiModel;


    public OutfitSuggestionService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api.key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4o-mini}") String openAiModel) {

        this.webClient = webClientBuilder.baseUrl(OPENAI_API_URL).build();
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
    }

    public String generateAgenticPrompt(SuggestionRequest request) {
        // Convert previous history into a numbered blacklist for the AI agent
        String historyBlacklist;
        if (request.history() == null || request.history().isEmpty()) {
            historyBlacklist = "  (none — this is the first request)";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < request.history().size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(request.history().get(i)).append("\n");
            }
            historyBlacklist = sb.toString();
        }

        String userNotes = (request.notes() == null || request.notes().isBlank())
                ? "(no special notes)"
                : request.notes();

        // Strict agentic prompt with Chain-of-Thought and hard constraints
        return String.format(
                "You are Fashion Sensor — a strict, opinionated AI stylist agent.\n\n" +
                "═══════════════════════════════════════════\n" +
                "HARD CONSTRAINTS (MUST OBEY — violating ANY = failure):\n" +
                "═══════════════════════════════════════════\n" +
                "1. LOCATION is \"%s\" — every outfit MUST be appropriate for this specific place's weather, culture, and social norms.\n" +
                "2. PURPOSE is \"%s\" — every outfit MUST serve this occasion. Do NOT suggest party wear for office or vice versa.\n" +
                "3. TARGET AUDIENCE is \"%s\" — respect the gender/audience for all recommendations.\n" +
                "4. STYLE VIBE is \"%s\" — match this aesthetic precisely.\n" +
                "5. USER NOTES: \"%s\" — these are ABSOLUTE RULES. If the user says 'avoid red', then ZERO red items. If they say 'prefer pastels', then ALL items must be pastel-toned. Never ignore this.\n\n" +
                "═══════════════════════════════════════════\n" +
                "BLACKLISTED OUTFITS (NEVER repeat or rephrase these):\n" +
                "═══════════════════════════════════════════\n" +
                "%s\n" +
                "If ANY of your suggestions overlap with a blacklisted outfit (even partially), replace it with something completely different.\n\n" +
                "═══════════════════════════════════════════\n" +
                "TASK:\n" +
                "═══════════════════════════════════════════\n" +
                "Think step by step:\n" +
                "A. Identify the location's climate, culture, and dress code norms.\n" +
                "B. Consider the purpose/occasion — what would look appropriate AND stylish?\n" +
                "C. Apply the user's notes as inviolable filters.\n" +
                "D. Cross-check each suggestion against the blacklist — if it matches, discard and invent a fresh one.\n" +
                "E. Output EXACTLY 3 unique, creative outfits.\n\n" +
                "Return ONLY valid JSON (no markdown, no commentary):\n" +
                "{ \"reasoning\": \"2-3 sentences explaining WHY these outfits suit this location + purpose + notes\", \"options\": [\"Full outfit 1 description\", \"Full outfit 2 description\", \"Full outfit 3 description\"] }",
                request.location(), request.purpose(), request.gender(),
                request.styleVibe(), userNotes, historyBlacklist);
    }

    public SuggestionResponse generateSuggestion(Map<String, String> requestData, MultipartFile photo) {
        Map<String, String> normalized = normalize(requestData);

        if (photo == null || photo.isEmpty()) {
            logger.error("Gemini Failure: No image provided. Text-only requests must use /api/agentic-suggest.");
            throw new RuntimeException("No image provided. Use the agentic-suggest endpoint for text-based outfit generation.");
        }

        if (!StringUtils.hasText(openAiApiKey)) {
            logger.error("Gemini Failure: OPENAI_API_KEY is not configured. Cannot analyze the uploaded image.");
            throw new RuntimeException("Image analysis is unavailable: OpenAI API key is not configured.");
        }

        try {
            SuggestionResponse aiSuggestion = generateImageSuggestion(normalized, photo);
            if (aiSuggestion != null) {
                return aiSuggestion;
            }
            throw new RuntimeException("OpenAI image analysis returned an empty response.");
        } catch (RuntimeException e) {
            logger.error("Gemini Failure: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Gemini Failure: ", e);
            throw new RuntimeException("Image analysis failed: " + e.getMessage(), e);
        }
    }

    private SuggestionResponse generateImageSuggestion(Map<String, String> requestData, MultipartFile photo)
            throws IOException {

        String audience = requestData.get("audience");
        String style = requestData.get("style");
        String purpose = requestData.get("purpose");
        String location = requestData.get("location");

        String base64 = Base64.getEncoder().encodeToString(photo.getBytes());
        String mimeType = StringUtils.hasText(photo.getContentType()) ? photo.getContentType()
                : MediaType.IMAGE_JPEG_VALUE;

        Map<String, Object> payload = buildVisionPayload(requestData, base64, mimeType);

        String rawResponse = webClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (!StringUtils.hasText(rawResponse)) {
            logger.error("Gemini Failure: OpenAI returned an empty response body for the image analysis request.");
            throw new RuntimeException("OpenAI returned an empty response for image analysis.");
        }

        JsonNode root = objectMapper.readTree(rawResponse);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            logger.error("Gemini Failure: OpenAI response missing content. raw={}", rawResponse);
            throw new RuntimeException("OpenAI response missing content field.");
        }

        JsonNode parsed = objectMapper.readTree(stripCodeFences(content));
        List<String> items = sanitizeItems(parsed.path("items"));
        if (items.isEmpty()) {
            logger.error("Gemini Failure: OpenAI response contained no clothing items. content={}", content);
            throw new RuntimeException("OpenAI response did not include any clothing items.");
        }

        String resolvedStyle = defaultIfBlank(parsed.path("style").asText(""), style);
        List<String> outfits = sanitizeItems(parsed.path("outfits"));
        if (outfits.isEmpty()) {
            logger.error("Gemini Failure: OpenAI response contained no outfit combinations. content={}", content);
            throw new RuntimeException("OpenAI response did not include any outfit combinations.");
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
                "ai-image");
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
                requestData.get("notes"));

        Map<String, Object> imageUrl = Map.of(
                "url", "data:" + mimeType + ";base64," + base64);

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", prompt);

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", imageUrl);

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent));

        String baseSystemContent = "You are a fashion stylist and vision assistant. Return valid JSON only.";
        String userNotes = requestData.get("notes");
        if (StringUtils.hasText(userNotes)) {
            baseSystemContent += " CRITICAL CONSTRAINT: The user explicitly noted: '" + userNotes
                    + "'. You MUST STRICTLY exclude any items, colors, or styles mentioned as a negative (e.g. 'no white', 'avoid red', 'without hat'). DO NOT include them in any response.";
        }

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", baseSystemContent);

        Map<String, Object> responseFormat = Map.of(
                "type", "json_object");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("temperature", 0.3);
        payload.put("response_format", responseFormat);
        payload.put("messages", List.of(systemMessage, userMessage));
        return payload;
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
