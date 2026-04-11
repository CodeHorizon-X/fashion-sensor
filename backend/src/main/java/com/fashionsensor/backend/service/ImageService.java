package com.fashionsensor.backend.service;

import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ImageService {

    public String generateImageUrl(String category, String style, String audience) {
        String query = String.format("%s %s %s fashion photography ultra realistic high quality", style, category, audience)
                .replaceAll("\\s+", " ")
                .trim();
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            // Using Pollinations AI parameter-driven prompt interface for dynamic image generation
            return "https://image.pollinations.ai/prompt/" + encodedQuery + "?nologo=true&width=600&height=800";
        } catch (Exception e) {
            // Safe fallback if encoding fails entirely
            return "https://image.pollinations.ai/prompt/high+fashion?nologo=true";
        }
    }
    
    public String generatePinterestImageUrl(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query + " fashion street style high quality", StandardCharsets.UTF_8.toString());
            return "https://image.pollinations.ai/prompt/" + encodedQuery + "?nologo=true&width=400&height=500";
        } catch (Exception e) {
            return "https://image.pollinations.ai/prompt/street+fashion?nologo=true";
        }
    }
}
