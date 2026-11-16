package com.portfolio.devprofile.dto;

import java.util.List;
import java.util.Map;

public record ChatResponse(Map<String, Object> message, String done) {
    public String text() {
        List<Map<String,Object>> content = (List<Map<String, Object>>) message.get("content");
        return content.stream()
                .filter(b -> "text".equals(b.get("type")))
                .map(b -> (String) b.get("text"))
                .findFirst()
                .orElse("");
    }
}

