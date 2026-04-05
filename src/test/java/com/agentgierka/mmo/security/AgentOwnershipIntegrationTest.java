package com.agentgierka.mmo.security;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Agent Ownership Security Integration Tests")
class AgentOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private UUID ownerAgentId;

    @BeforeEach
    void setUp() {
        // Create Owner in DB
        Player owner = Player.create("ownerUser", "password");
        playerRepository.save(owner);

        // Create Another Player
        Player other = Player.create("otherUser", "password");
        playerRepository.save(other);

        // Link Agent to Owner
        Agent agent = Agent.builder()
                .name("Owner's Agent")
                .owner(owner)
                .status(AgentStatus.IDLE)
                .x(0).y(0)
                .build();
        ownerAgentId = agentRepository.save(agent).getId();
    }

    @Test
    @DisplayName("Should allow movement if user is the owner")
    void shouldAllowMovementForOwner() throws Exception {
        mockMvc.perform(post("/api/agents/" + ownerAgentId + "/move")
                        .with(user("ownerUser"))
                        .param("x", "10")
                        .param("y", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 Forbidden if user is NOT the owner")
    void shouldReturn403ForNonOwner() throws Exception {
        mockMvc.perform(post("/api/agents/" + ownerAgentId + "/move")
                        .with(user("otherUser"))
                        .param("x", "10")
                        .param("y", "10"))
                .andExpect(status().isForbidden());
    }
}
