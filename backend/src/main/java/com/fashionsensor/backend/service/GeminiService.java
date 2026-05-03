package com.fashionsensor.backend.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    
    // Using the stable model identifier for the v1 endpoint
  private static final String GEMINI_MODEL = "gemini-pro";
    
    private final String geminiApiKey;
    private Client geminiClient;

    public GeminiService(
            @Value("${gemini.api.key:}") String geminiApiKey) {

        String resolvedKey = StringUtils.hasText(geminiApiKey)
                ? geminiApiKey
                : System.getenv("GEMINI_API_KEY");

        if (!StringUtils.hasText(resolvedKey)) {
            logger.error("CRITICAL: GEMINI_API_KEY is missing. Check your .env file.");
        } else {
            // Simplified initialization
            this.geminiClient = Client.builder().apiKey(resolvedKey).build();
            logger.info("GeminiService initialized successfully.");
        }

        this.geminiApiKey = resolvedKey;
    }

    public boolean isAvailable() {
        return StringUtils.hasText(geminiApiKey) && geminiClient != null;
    }

    public String generateContent(String prompt) {
        if (!isAvailable()) {
            logger.error("Gemini API call skipped: Client not initialized.");
            return null;
        }

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.9f)
                    .responseMimeType("application/json")
                    .build();

            // Direct call to models.generateContent
            GenerateContentResponse response = geminiClient.models.generateContent(
                    GEMINI_MODEL,
                    prompt,
                    config
            );

            String text = response.text();
            if (StringUtils.hasText(text)) {
                logger.debug("Received content from Gemini.");
                return text;
            }
            
            return null;

        } catch (Exception e) {
            logger.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("Agentic suggestion failed: " + e.getMessage(), e);
        }
    }
}