package com.example.pastebin.controller;

import com.example.pastebin.model.PasteRequest;
import com.example.pastebin.model.PasteResponse;
import com.example.pastebin.service.PasteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "pastebin", description = "Create and resolve short links")
public class PasteController {

    private final PasteService service;

    public PasteController(PasteService service) {
        this.service = service;
    }

    @GetMapping("/")
    @Operation(summary = "Service banner")
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(
                Map.of("service", "pastebin-java", "docs", "/swagger-ui.html"));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/paste")
    @Operation(summary = "Create a short link")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<?> create(@RequestBody(required = false) PasteRequest request) {
        String url = request == null ? null : request.url();

        if (url == null || url.isBlank()) {
            return badRequest("url is required");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return badRequest("url must be a valid http(s) URL");
        }

        PasteResponse response = service.create(url);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Resolve a short link")
    @ApiResponse(responseCode = "302", description = "Redirect to the long URL")
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = Map.class)))
    public ResponseEntity<?> resolve(@PathVariable String code) {
        if (!isValidCode(code)) {
            return notFound();
        }

        Optional<String> longUrl = service.resolve(code);
        if (longUrl.isEmpty()) {
            return notFound();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl.get()))
                .build();
    }

    private boolean isValidCode(String code) {
        if (code == null || code.length() != 8) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            boolean alphanumeric =
                    (c >= 'A' && c <= 'Z')
                            || (c >= 'a' && c <= 'z')
                            || (c >= '0' && c <= '9');
            if (!alphanumeric) {
                return false;
            }
        }
        return true;
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not found"));
    }
}
