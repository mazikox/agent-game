package com.agentgierka.mmo.security;

import com.agentgierka.mmo.player.web.dto.LoginRequest;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Unhappy Path Integration Tests")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return 401 when Authorization header is missing")
    void shouldReturn401WhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/agents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header is malformed")
    void shouldReturn401WhenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/agents")
                        .header("Authorization", "Bearer not-a-valid-jwt-structure"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when token is signed with a different secret")
    void shouldReturn401WhenTokenIsSignedWithWrongSecret() throws Exception {
        String tokenWithWrongSecret = JWT.create()
                .withSubject("user")
                .withExpiresAt(new Date(System.currentTimeMillis() + 100000))
                .sign(Algorithm.HMAC256("completely-different-and-wrong-secret-key"));

        mockMvc.perform(get("/api/v1/agents")
                        .header("Authorization", "Bearer " + tokenWithWrongSecret))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when login credentials are incorrect")
    void shouldReturn401WhenLoginPasswordIsIncorrect() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent_user", "wrong_password");
        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
