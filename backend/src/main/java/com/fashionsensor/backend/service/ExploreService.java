package com.fashionsensor.backend.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExploreService {

    private final ImageService imageService;

    public ExploreService(ImageService imageService) {
        this.imageService = imageService;
    }

    public List<Map<String, String>> fetchExploreItems(String audience, String style) {
        List<Map<String, String>> items = new ArrayList<>();
        
        String safeAudience = (audience == null || audience.isBlank()) ? "men" : audience.toLowerCase();
        String safeStyle = (style == null || style.isBlank()) ? "all" : style.toLowerCase();
        
        // Return 6 dynamically generated explore items
        int itemsToGenerate = 6;
        for (int i = 0; i < itemsToGenerate; i++) {
            Map<String, String> item = new HashMap<>();
            
            String itemStyle = safeStyle.equals("all") ? getRandomStyle(i) : safeStyle;
            
            String title = generateTitle(itemStyle, safeAudience, i);
            String description = generateDescription(itemStyle, safeAudience, i);
            String image = imageService.generateImageUrl(title, itemStyle, safeAudience);
            
            item.put("title", title);
            // Description holds placeholder styling text, can be dynamic
            item.put("description", description);
            item.put("audience", safeAudience);
            item.put("style", itemStyle);
            item.put("image", image);
            
            items.add(item);
        }
        
        return items;
    }
    
    private String getRandomStyle(int index) {
        String[] styles = {"casual", "minimal", "genz", "formal", "athleisure"};
        return styles[index % styles.length];
    }
    
    private String generateTitle(String style, String audience, int index) {
        String prefix = audience.equals("kids") ? "Mini " : "";
        switch (style) {
            case "casual": return prefix + "Off-Duty Layers " + (index + 1);
            case "minimal": return prefix + "Soft Minimalism " + (index + 1);
            case "genz": return prefix + "Trending Street Mix " + (index + 1);
            case "formal": return prefix + "Refined Edit " + (index + 1);
            case "athleisure": return prefix + "Sporty Chic " + (index + 1);
            default: return prefix + "Everyday Look " + (index + 1);
        }
    }
    
    private String generateDescription(String style, String audience, int index) {
        if (audience.equals("kids")) {
            return "Playful layers and comfortable materials perfect for active days.";
        }
        switch (style) {
            case "casual": return "Relaxed silhouettes with clean basics and soft neutrals for polished daily wear.";
            case "minimal": return "Structured basics, monochrome layers, and calm textures for sharp modern dressing.";
            case "genz": return "Boxy fits, volume silhouettes, and statement pieces for trend-first styling.";
            case "formal": return "Crisp separates, sharp tailoring, and smart footwear for office-ready confidence.";
            case "athleisure": return "Performance wear seamlessly mixed with lifestyle pieces for maximum comfort.";
            default: return "A uniquely curated style to match your everyday rhythm.";
        }
    }
}
