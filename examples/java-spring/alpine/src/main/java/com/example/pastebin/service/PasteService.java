package com.example.pastebin.service;

import com.example.pastebin.model.PasteResponse;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PasteService {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 8;

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public PasteResponse create(String longUrl) {
        String code;
        do {
            code = randomCode();
        } while (store.putIfAbsent(code, longUrl) != null);
        return new PasteResponse(code, "/" + code, longUrl);
    }

    public Optional<String> resolve(String code) {
        return Optional.ofNullable(store.get(code));
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
