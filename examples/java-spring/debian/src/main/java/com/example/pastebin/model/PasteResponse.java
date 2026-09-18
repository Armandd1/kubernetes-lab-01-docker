package com.example.pastebin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned when a short link is created")
public record PasteResponse(
        @Schema(description = "Random 8-character alphanumeric code", example = "aB3dE9xZ")
        String code,

        @Schema(description = "Short path that redirects to the long URL", example = "/aB3dE9xZ")
        @JsonProperty("short_url")
        String shortUrl,

        @Schema(description = "The original long URL", example = "https://example.com/a/very/long/link")
        @JsonProperty("long_url")
        String longUrl) {
}
