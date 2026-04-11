package com.fashionsensor.backend.model;

import java.util.List;
import java.util.Map;

public record SuggestionResponse(
        String outfit,
        List<String> outfits,
        List<String> items,
        List<SuggestionItem> itemCards,
        String style,
        Map<String, String> amazonLinks,
        String pinterestQuery,
        String audience,
        String source
) {
}
