package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.web.dto.AssignGoalRequest;
import com.agentgierka.mmo.agent.web.dto.MoveRequest;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.security.AgentSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@WithMockUser
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @MockitoBean
    private AgentSecurity agentSecurity;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID agentId;
    private Player owner;

    @BeforeEach
    void setUp() {
        agentRepository.deleteAll();
        playerRepository.deleteAll();

        owner = Player.create("testPlayer", "password");
        playerRepository.save(owner);

        Agent agent = Agent.create("TestAgent", owner, null, 10, 10, 1);
        agentRepository.save(agent);
        agentId = agent.getId();

        // Ensure security check always passes during validation testing
        when(agentSecurity.isOwner(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Should assign goal and return UPDATED agent (L3 Fix)")
    void assignGoal_Success() throws Exception {
        AssignGoalRequest request = new AssignGoalRequest("New Mission");

        mockMvc.perform(post("/api/v1/agents/{id}/goal", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("New Mission"));
    }

    @Test
    @DisplayName("Should return 400 when goal is blank (H5 Validation)")
    void assignGoal_ValidationFailure() throws Exception {
        AssignGoalRequest request = new AssignGoalRequest("");

        mockMvc.perform(post("/api/v1/agents/{id}/goal", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when move coordinates are negative (H5 Validation)")
    void move_ValidationFailure() throws Exception {
        MoveRequest request = new MoveRequest(-1, 50);

        mockMvc.perform(post("/api/v1/agents/{id}/move", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when status is invalid (H5 Validation)")
    void updateStatus_InvalidStatus() throws Exception {
        String invalidJson = "{\"status\": \"INVALID_STATUS\", \"description\": \"Test\"}";

        mockMvc.perform(post("/api/v1/agents/{id}/status", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
