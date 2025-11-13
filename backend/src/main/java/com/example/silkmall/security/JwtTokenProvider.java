package com.example.silkmall.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Component
public class JwtTokenProvider {
    
    @Value("${app.jwtSecret}")
    private String jwtSecret;
    
    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    public long getJwtExpirationInSeconds() {
        return jwtExpirationInMs / 1000L;
    }
    
    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getId());
        claims.put("username", userDetails.getUsername());
        claims.put("userType", userDetails.getUserType());
        
        SecretKey key = resolveSigningKey();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public Long getUserIdFromJWT(String token) {
        SecretKey key = resolveSigningKey();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return Long.parseLong(claims.get("userId").toString());
    }
    
    public boolean validateToken(String authToken) {
        try {
            SecretKey key = resolveSigningKey();
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            // 各种异常处理：签名过期、令牌错误等
            return false;
        }
    }

    private SecretKey resolveSigningKey() {
        byte[] keyBytes = decodeSecret(jwtSecret);
        if (keyBytes.length < HS512_MIN_KEY_BYTES) {
            keyBytes = deriveHs512Key(jwtSecret);
        }
        if (keyBytes.length < HS512_MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT secret derivation did not yield a secure key length");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }

    private byte[] decodeSecret(String secret) {
        if (secret == null) {
            throw new IllegalStateException("JWT secret cannot be null");
        }
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException | io.jsonwebtoken.io.DecodingException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] deriveHs512Key(String secret) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), KEY_DERIVATION_SALT, KEY_DERIVATION_ITERATIONS, HS512_MIN_KEY_BYTES * 8);
            byte[] derived = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return derived;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to derive a secure JWT signing key", e);
        }
    }

    private static final int HS512_MIN_KEY_BYTES = 64;
    private static final int KEY_DERIVATION_ITERATIONS = 120_000;
    private static final byte[] KEY_DERIVATION_SALT =
            "com.example.silkmall.jwt.key-derivation".getBytes(StandardCharsets.UTF_8);
}
