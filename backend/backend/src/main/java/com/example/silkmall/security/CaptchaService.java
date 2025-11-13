package com.example.silkmall.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {
    private static final long DEFAULT_TTL_SECONDS = 120;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public CaptchaChallenge createChallenge() {
        cleanupExpired();
        int left = random.nextInt(8) + 2;
        int right = random.nextInt(8) + 1;
        boolean addition = random.nextBoolean();
        int result = addition ? left + right : left * right;
        String operator = addition ? " + " : " × ";

        String id = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);
        challenges.put(id, new Challenge(String.valueOf(result), expiresAt));

        String question = left + operator + right + " = ?";
        return new CaptchaChallenge(id, question, DEFAULT_TTL_SECONDS);
    }

    public boolean validate(String challengeId, String answer) {
        if (challengeId == null || challengeId.isBlank() || answer == null || answer.isBlank()) {
            return false;
        }

        Challenge challenge = challenges.remove(challengeId);
        if (challenge == null) {
            return false;
        }

        if (challenge.expiresAt().isBefore(Instant.now())) {
            return false;
        }

        return challenge.solution().equals(answer.trim());
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record CaptchaChallenge(String id, String question, long expiresInSeconds) {}

    private record Challenge(String solution, Instant expiresAt) {}
}
