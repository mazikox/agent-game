package com.agentgierka.mmo.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.ms:86400000}") // 24 hours
    private long expirationTimeMs;

    @PostConstruct
    public void validateSecret() {
        if (secretKey == null || secretKey.isBlank() || secretKey.startsWith("$")) {
            throw new IllegalStateException("JWT_SECRET is not configured or uses a placeholder!");
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters for HMAC256!");
        }
    }

    public String generateToken(UserDetails userDetails) {
        return JWT.create()
                .withIssuer("agent-game")
                .withAudience("agent-game-api")
                .withSubject(userDetails.getUsername())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTimeMs))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public String extractUsername(String token) {
        return verifyToken(token).getSubject();
    }

    private DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey))
                .withIssuer("agent-game")
                .withAudience("agent-game-api")
                .build();
        return verifier.verify(token);
    }
}
