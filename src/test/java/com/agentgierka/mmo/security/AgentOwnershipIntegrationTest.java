package com.agentgierka.mmo.security;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.agent.web.dto.MoveRequest;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

    @Autowired
    private LocationRepository locationRepository;

    @MockitoBean
    private AgentWorldStateRepository agentWorldStateRepository;

    @MockitoBean
    private CreatureInstanceRepository creatureInstanceRepository;

    @MockitoBean
    private WorldStateSynchronizer worldStateSynchronizer;

    @MockitoBean
    private AgentSecurity agentSecurity;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID ownerAgentId;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        playerRepository.deleteAll();
        agentRepository.deleteAll();

        // Create Location
        Location forest = Location.builder()
                .name("Security Forest")
                .width(100).height(100)
                .build();
        locationRepository.save(forest);

        // Create Owner in DB
        Player owner = Player.create("ownerUser", "password");
        playerRepository.save(owner);

        // Create Another Player
        Player other = Player.create("otherUser", "password");
        playerRepository.save(other);

        // Link Agent to Owner
        Agent agent = Agent.create("Owner's Agent", owner, forest, 0, 0, 1);
        ownerAgentId = agentRepository.save(agent).getId();
        
        // Ensure data is visible
        agentRepository.flush();

        // Mock security behavior to actually test the controller's use of security, 
        // bypassing the Redis @Cacheable aspect in the real bean.
        when(agentSecurity.isOwner(eq(ownerAgentId), eq("ownerUser"))).thenReturn(true);
        when(agentSecurity.isOwner(eq(ownerAgentId), eq("otherUser"))).thenReturn(false);
    }

    @Test
    @DisplayName("Should allow movement if user is the owner")
    void shouldAllowMovementForOwner() throws Exception {
        MoveRequest moveRequest = new MoveRequest(10, 10);
        mockMvc.perform(post("/api/v1/agents/" + ownerAgentId + "/move")
                        .with(user("ownerUser"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 Forbidden if user is NOT the owner")
    void shouldReturn403ForNonOwner() throws Exception {
        MoveRequest moveRequest = new MoveRequest(10, 10);
        mockMvc.perform(post("/api/v1/agents/" + ownerAgentId + "/move")
                        .with(user("otherUser"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isForbidden());
    }
}
