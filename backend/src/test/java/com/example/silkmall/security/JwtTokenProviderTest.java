package com.example.silkmall.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JwtTokenProviderTest {

    @Test
    void generateTokenWithShortPlaintextSecret() {
        JwtTokenProvider provider = buildProvider("tinySecret");
        SecretKey signingKey = ReflectionTestUtils.invokeMethod(provider, "resolveSigningKey");
        assertThat(signingKey).isNotNull();
        assertThat(signingKey.getEncoded().length).isGreaterThanOrEqualTo(64);

        UsernamePasswordAuthenticationToken authentication = buildAuthentication();
        String token = assertDoesNotThrow(() -> provider.generateToken(authentication));
        assertThat(token).isNotBlank();
        assertThat(provider.getUserIdFromJWT(token)).isEqualTo(42L);
    }

    @Test
    void generateTokenWithShortBase64Secret() {
        String base64Secret = Base64.getEncoder().encodeToString("shortBase64".getBytes(StandardCharsets.UTF_8));
        JwtTokenProvider provider = buildProvider(base64Secret);
        SecretKey signingKey = ReflectionTestUtils.invokeMethod(provider, "resolveSigningKey");
        assertThat(signingKey).isNotNull();
        assertThat(signingKey.getEncoded().length).isGreaterThanOrEqualTo(64);

        UsernamePasswordAuthenticationToken authentication = buildAuthentication();
        String token = assertDoesNotThrow(() -> provider.generateToken(authentication));
        assertThat(token).isNotBlank();
        assertThat(provider.getUserIdFromJWT(token)).isEqualTo(42L);
    }

    private JwtTokenProvider buildProvider(String secret) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", 3_600_000);
        return provider;
    }

    private UsernamePasswordAuthenticationToken buildAuthentication() {
        CustomUserDetails details = new CustomUserDetails(
                42L,
                "demoUser",
                "password",
                "demo@example.com",
                "18800001111",
                "consumer",
                true,
                Collections.emptyList()
        );
        return new UsernamePasswordAuthenticationToken(details, details.getPassword(), details.getAuthorities());
    }
}
