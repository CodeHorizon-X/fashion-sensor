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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OutfitSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(OutfitSuggestionService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final int MAX_RATE_LIMIT_RETRIES = 2;

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
        this.openAiModel = defaultIfBlank(openAiModel, "gpt-4o-mini");
    }

    public SuggestionResponse generateSuggestion(Map<String, String> data, MultipartFile photo) {
        Map<String, String> normalized = normalize(data, photo);
        String audience = normalized.get("audience");
        String style = normalized.get("style");

        try {
            String content = callOpenAi(buildOutfitPrompt(normalized, Collections.emptyList()));
            OpenAiOutfitResult result = parseOpenAiContent(content);

            if (result.options().isEmpty()) {
                logger.error("OpenAI response parsed successfully but contained no options. content={}", content);
                return minimalResponse(audience, style);
            }

            return toSuggestionResponse(result, audience, style);
        } catch (OpenAiRateLimitException e) {
            logger.warn("OpenAI rate limit while generating standard suggestion. Returning empty AI response.", e);
            return minimalResponse(audience, style);
        } catch (Exception e) {
            logger.error("OpenAI outfit suggestion failed. Returning API-failure fallback.", e);
            return fallbackResponse(audience, style);
        }
    }

    public Map<String, Object> generateAgenticSuggestion(SuggestionRequest request) {
        Map<String, String> normalized = normalize(request);

        try {
            String content = callOpenAi(buildOutfitPrompt(normalized, safeHistory(request)));
            OpenAiOutfitResult result = parseOpenAiContent(content);

            if (result.options().isEmpty()) {
                logger.error("OpenAI agentic response parsed successfully but contained no options. content={}", content);
                return Map.of(
                        "reasoning", "",
                        "options", List.of());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("reasoning", result.reasoning());
            response.put("options", result.options());
            return response;
        } catch (OpenAiRateLimitException e) {
            logger.warn("OpenAI rate limit while generating agentic suggestion. Returning safe empty options.", e);
            return safeEmptyOptions();
        } catch (Exception e) {
            logger.error("OpenAI agentic suggestion failed.", e);
            throw new RuntimeException("OpenAI suggestion failed: " + e.getMessage(), e);
        }
    }

    private String callOpenAi(String prompt) throws Exception {
        if (!StringUtils.hasText(openAiApiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("messages", List.of(message));

        String rawResponse = postOpenAiWithRetry(payload);

        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalStateException("OpenAI returned an empty response body.");
        }

        JsonNode root = objectMapper.readTree(rawResponse);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        logger.info("OpenAI RAW message.content: {}", content);
        content = content.replace("```json", "")
                 .replace("```", "")
                 .trim();

         if (!StringUtils.hasText(content)) {
            logger.warn("OpenAI returned empty content");
            return "{}"; // safe fallback, but doesn't crash
        }

        return content;
    }

    private String postOpenAiWithRetry(Map<String, Object> payload) {
        for (int attempt = 0; attempt <= MAX_RATE_LIMIT_RETRIES; attempt++) {
            try {
                return webClient.post()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (WebClientResponseException.TooManyRequests e) {
                logRateLimit(attempt, e);
                if (attempt == MAX_RATE_LIMIT_RETRIES) {
                    throw new OpenAiRateLimitException("OpenAI rate limit after retries.", e);
                }
                sleepBeforeRetry(attempt);
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 429) {
                    logRateLimit(attempt, e);
                    if (attempt == MAX_RATE_LIMIT_RETRIES) {
                        throw new OpenAiRateLimitException("OpenAI rate limit after retries.", e);
                    }
                    sleepBeforeRetry(attempt);
                } else {
                    throw e;
                }
            }
        }

        throw new OpenAiRateLimitException("OpenAI rate limit retry loop ended unexpectedly.", null);
    }

    private void logRateLimit(int attempt, WebClientResponseException e) {
        logger.warn("OpenAI rate limit hit. attempt={}/{} status={} body={}",
                attempt + 1,
                MAX_RATE_LIMIT_RETRIES + 1,
                e.getStatusCode().value(),
                e.getResponseBodyAsString());
    }

    private void sleepBeforeRetry(int attempt) {
        long delayMs = (attempt + 1L) * 1000L;
        logger.warn("Retrying OpenAI request after {}ms due to rate limit.", delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenAiRateLimitException("Interrupted while waiting to retry OpenAI request.", e);
        }
    }

    private Map<String, Object> safeEmptyOptions() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reasoning", "");
        response.put("options", List.of());
        return response;
    }

    private String buildOutfitPrompt(Map<String, String> normalized, List<String> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate 3 outfit suggestions in JSON format with fields: title, top, bottom, footwear, accessories, style.\n");
        prompt.append("Return ONLY valid JSON. Do NOT include markdown or explanations outside JSON.\n");
        prompt.append("Use exactly this shape:\n");
        prompt.append("{\"options\":[{\"title\":\"Outfit 1\",\"top\":\"string\",\"bottom\":\"string\",\"footwear\":\"string\",\"accessories\":[\"string\"],\"style\":\"casual\"}]}\n\n");
        prompt.append("User context:\n");
        prompt.append("Gender: ").append(normalized.get("audience")).append("\n");
        prompt.append("Occasion: ").append(normalized.get("occasion")).append("\n");
        prompt.append("Weather: ").append(normalized.get("weather")).append("\n");
        prompt.append("Wardrobe: ").append(normalized.get("wardrobe")).append("\n");

        if (!history.isEmpty()) {
            prompt.append("Avoid repeating these previous outfits:\n");
            for (int i = 0; i < history.size(); i++) {
                prompt.append(i + 1).append(". ").append(history.get(i)).append("\n");
            }
        }

        return prompt.toString();
    }

    private OpenAiOutfitResult parseOpenAiContent(String content) throws Exception {
        String cleaned = stripCodeFences(content);
        JsonNode root = objectMapper.readTree(cleaned);
        JsonNode optionsNode = root.path("options");

        if (!optionsNode.isArray()) {
            throw new IllegalArgumentException("OpenAI JSON does not contain an options array.");
        }

        List<String> options = new ArrayList<>();
        Set<String> items = new LinkedHashSet<>();
        String style = "";

        for (JsonNode optionNode : optionsNode) {
            if (optionNode.isTextual()) {
                String text = optionNode.asText("").trim();
                if (StringUtils.hasText(text)) {
                    options.add(text);
                    items.addAll(splitIntoItems(text));
                }
                continue;
            }

            if (!optionNode.isObject()) {
                continue;
            }

            String title = optionNode.path("title").asText("");
            String top = optionNode.path("top").asText("");
            String bottom = optionNode.path("bottom").asText("");
            String footwear = optionNode.path("footwear").asText("");
            String optionStyle = optionNode.path("style").asText("");
            List<String> accessories = readAccessories(optionNode.path("accessories"));

            addIfPresent(items, top);
            addIfPresent(items, bottom);
            addIfPresent(items, footwear);
            accessories.forEach(accessory -> addIfPresent(items, accessory));

            if (StringUtils.hasText(optionStyle)) {
                style = optionStyle.trim();
            }

            String outfit = buildOutfitText(title, top, bottom, footwear, accessories);
            if (StringUtils.hasText(outfit)) {
                options.add(outfit);
            }
        }

        return new OpenAiOutfitResult(
                root.path("reasoning").asText(""),
                options,
                new ArrayList<>(items),
                style);
    }

    private SuggestionResponse toSuggestionResponse(OpenAiOutfitResult result, String audience, String fallbackStyle) {
        String style = defaultIfBlank(result.style(), fallbackStyle);
        List<String> items = result.items();
        String primaryOutfit = result.options().get(0);

        return new SuggestionResponse(
                primaryOutfit,
                result.options(),
                items,
                style,
                buildAmazonLinks(items, audience),
                buildPinterestQuery(style, audience),
                audience,
                "openai");
    }

    private SuggestionResponse minimalResponse(String audience, String style) {
        return new SuggestionResponse(
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                defaultIfBlank(style, "casual"),
                Collections.emptyMap(),
                buildPinterestQuery(style, audience),
                defaultIfBlank(audience, "men"),
                "openai");
    }

    private SuggestionResponse fallbackResponse(String audience, String style) {
        List<String> outfits = List.of(
                "Reliable casual outfit: breathable t-shirt, straight-fit jeans, clean sneakers, and a simple watch");
        List<String> items = List.of("breathable t-shirt", "straight-fit jeans", "clean sneakers", "simple watch");
        String resolvedStyle = defaultIfBlank(style, "casual");

        return new SuggestionResponse(
                outfits.get(0),
                outfits,
                items,
                resolvedStyle,
                buildAmazonLinks(items, audience),
                buildPinterestQuery(resolvedStyle, audience),
                defaultIfBlank(audience, "men"),
                "fallback");
    }

    private Map<String, String> normalize(Map<String, String> source, MultipartFile photo) {
        Map<String, String> normalized = new LinkedHashMap<>();
        String audience = defaultIfBlank(getValue(source, "audience"), "men");
        String purpose = defaultIfBlank(getValue(source, "purpose"), "casual");
        String style = defaultIfBlank(getValue(source, "style"), "casual");
        String location = defaultIfBlank(getValue(source, "location"), "not specified");
        String notes = defaultIfBlank(getValue(source, "notes"), "");
        String withWhom = defaultIfBlank(getValue(source, "withWhom"), "");
        String date = defaultIfBlank(getValue(source, "date"), "");
        String weather = defaultIfBlank(getValue(source, "weather"), "normal");
        String wardrobe = defaultIfBlank(getValue(source, "items"), "");

        if (!StringUtils.hasText(wardrobe)) {
            List<String> context = new ArrayList<>();
            context.add("Style vibe: " + style);
            context.add("Location: " + location);
            if (StringUtils.hasText(withWhom)) context.add("Going with: " + withWhom);
            if (StringUtils.hasText(date)) context.add("Date/time: " + date);
            if (StringUtils.hasText(notes)) context.add("User notes: " + notes);
            if (photo != null && !photo.isEmpty()) {
                context.add("Wardrobe photo uploaded: " + photo.getOriginalFilename());
            } else {
                context.add("Wardrobe photo: not uploaded");
            }
            wardrobe = String.join("; ", context);
        }

        normalized.put("audience", audience);
        normalized.put("occasion", buildOccasion(purpose, style, notes));
        normalized.put("style", style);
        normalized.put("weather", weather);
        normalized.put("wardrobe", wardrobe);
        return normalized;
    }

    private Map<String, String> normalize(SuggestionRequest request) {
        String audience = defaultIfBlank(request.gender(), "men");
        String purpose = defaultIfBlank(request.purpose(), "casual");
        String style = defaultIfBlank(request.styleVibe(), "casual");
        String location = defaultIfBlank(request.location(), "not specified");
        String notes = defaultIfBlank(request.notes(), "");

        Map<String, String> normalized = new LinkedHashMap<>();
        normalized.put("audience", audience);
        normalized.put("occasion", buildOccasion(purpose, style, notes));
        normalized.put("style", style);
        normalized.put("weather", "normal");
        normalized.put("wardrobe", "Location: " + location + (StringUtils.hasText(notes) ? "; User notes: " + notes : ""));
        return normalized;
    }

    private List<String> safeHistory(SuggestionRequest request) {
        return request.history() == null ? Collections.emptyList() : request.history();
    }

    private String buildOccasion(String purpose, String style, String notes) {
        String occasion = defaultIfBlank(purpose, "casual");
        if (StringUtils.hasText(style)) {
            occasion += " / " + style.trim();
        }
        if (StringUtils.hasText(notes)) {
            occasion += " / notes: " + notes.trim();
        }
        return occasion;
    }

    private String getValue(Map<String, String> source, String key) {
        if (source == null) {
            return "";
        }
        String value = source.get(key);
        return value == null ? "" : value.trim();
    }

    private String buildOutfitText(String title, String top, String bottom, String footwear, List<String> accessories) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(top)) pieces.add(top.trim());
        if (StringUtils.hasText(bottom)) pieces.add(bottom.trim());
        if (StringUtils.hasText(footwear)) pieces.add(footwear.trim());
        pieces.addAll(accessories);

        String body = String.join(", ", pieces);
        String resolvedTitle = defaultIfBlank(title, "");
        if (StringUtils.hasText(resolvedTitle) && StringUtils.hasText(body)) {
            return resolvedTitle + ": " + body;
        }
        if (StringUtils.hasText(resolvedTitle)) {
            return resolvedTitle;
        }
        return body;
    }

    private List<String> readAccessories(JsonNode accessoriesNode) {
        if (!accessoriesNode.isArray()) {
            return Collections.emptyList();
        }

        List<String> accessories = new ArrayList<>();
        for (JsonNode accessoryNode : accessoriesNode) {
            String accessory = accessoryNode.asText("").trim();
            if (StringUtils.hasText(accessory)) {
                accessories.add(accessory);
            }
        }
        return accessories;
    }

    private void addIfPresent(Set<String> items, String value) {
        if (StringUtils.hasText(value)) {
            items.add(value.trim());
        }
    }

    private List<String> splitIntoItems(String outfit) {
        if (!StringUtils.hasText(outfit)) return Collections.emptyList();
        String[] parts = outfit.split("[,+&]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (StringUtils.hasText(trimmed)) result.add(trimmed);
        }
        return result;
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
            trimmed = trimmed.replaceFirst("(?s)^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("(?s)\\s*```$", "");
        }
        return trimmed.trim();
    }

    private record OpenAiOutfitResult(
            String reasoning,
            List<String> options,
            List<String> items,
            String style) {
    }

    private static class OpenAiRateLimitException extends RuntimeException {
        OpenAiRateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
