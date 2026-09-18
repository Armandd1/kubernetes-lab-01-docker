package com.example.pastebin.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a short link")
public record PasteRequest(
        @Schema(
                description = "The long URL to shorten. Must start with http:// or https://",
                example = "https://example.com/a/very/long/link",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String url) {
}
