package com.fashionsensor.backend.model;

import java.util.List;

public record SuggestionRequest(
        String gender,
        String purpose,
        String styleVibe,
        String location,
        String notes,
        List<String> history // The Agent's memory
) {
}