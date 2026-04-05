package com.agentgierka.mmo.agent;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.engine.GameEngine;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.agent.web.AgentController;
import com.agentgierka.mmo.ai.port.Brain;
import com.agentgierka.mmo.world.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Golden E2E Test: The Great Escape")
class AgentMovementE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentController agentController;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PortalRepository portalRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private AgentWorldStateRepository agentWorldStateRepository;

    @MockitoBean
    private Brain brain;

    @BeforeEach
    void setup() {

        transactionTemplate.executeWithoutResult(s -> {
            portalRepository.deleteAll();
            agentRepository.deleteAll();
            playerRepository.deleteAll();
            locationRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("Agent should move through ticks and trigger portal upon arrival")
    void shouldMoveAndTriggerPortal() throws Exception {
        // 1. SETUP: Create World
        Location forest = saveLocation("E2E Forest", 100, 100);
        Location meadow = saveLocation("E2E Meadow", 100, 100);

        // Portal at (2, 2) leads to Meadow (5, 5)
        savePortal(forest, 2, 2, meadow, 5, 5);

        // Agent at (0, 0) with speed 1
        Agent agent = saveAgent("Explorer", forest, 0, 0, 1);
        UUID agentId = agent.getId();

        // Prepare Mock for Redis
        AgentWorldState initialState = AgentWorldState.builder()
                .agentId(agentId).x(0).y(0).targetX(2).targetY(2).status(AgentStatus.MOVING).speed(1).build();
        
        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of(initialState));

        // 2. ACT: Trigger Movement via API
        mockMvc.perform(post("/api/agents/" + agentId + "/move")
                .with(user("ExplorerPlayer"))
                .param("x", "2")
                .param("y", "2"))
                .andExpect(status().isOk());

        // 3. PROCESS: Tick 1 (Move to 1, 1)
        gameEngine.tick();
        
        // Redis should be updated with new position
        verify(agentWorldStateRepository, atLeastOnce()).saveAll(anyList());
        
        // 4. PROCESS: Tick 2 (Move to 2, 2 -> Trigger Portal)
        // Update mock to reflect current state before next tick using builder for immutability
        initialState = initialState.toBuilder().x(1).y(1).build();
        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of(initialState));
        
        gameEngine.tick();

        // Give a moment for asynchronous portal processing (Virtual Threads) to commit in DB
        Thread.sleep(100);

        // 5. VERIFY: Final State in Postgres
        transactionTemplate.executeWithoutResult(s -> {
            Agent updatedAgent = agentRepository.findById(agentId).orElseThrow();
            assertEquals("E2E Meadow", updatedAgent.getCurrentLocation().getName());
            assertEquals(5, updatedAgent.getX());
            assertEquals(5, updatedAgent.getY());
            assertEquals(AgentStatus.IDLE, updatedAgent.getStatus());
        });
    }

    private Location saveLocation(String name, int w, int h) {
        return transactionTemplate.execute(s -> locationRepository.save(
            Location.builder().name(name).width(w).height(h).type(LocationType.FOREST).build()
        ));
    }

    private void savePortal(Location src, int sx, int sy, Location dst, int dx, int dy) {
        transactionTemplate.execute(s -> portalRepository.save(
            Portal.builder().sourceLocation(src).sourceX(sx).sourceY(sy).targetLocation(dst).targetX(dx).targetY(dy).build()
        ));
    }

    private Agent saveAgent(String name, Location loc, int x, int y, int speed) {
        Player player = transactionTemplate.execute(s -> playerRepository.save(
            Player.create(name + "Player", "secret")
        ));
        return transactionTemplate.execute(s -> agentRepository.save(
            Agent.builder().name(name).owner(player).currentLocation(loc).x(x).y(y).speed(speed).status(AgentStatus.IDLE).build()
        ));
    }
}
