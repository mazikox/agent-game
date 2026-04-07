package com.agentgierka.mmo.world;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.ai.port.Brain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for the Portal system.
 * Verifies that arrival events trigger teleportation via the event listener.
 * Configuration is picked up from src/test/resources/application.properties.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Portal Integration Tests")
class PortalIntegrationTest {


    @Autowired
    private AgentPersistenceService agentPersistenceService;

    @MockitoBean
    private AgentWorldStateRepository agentWorldStateRepository; // Mocking Redis dependency

    @MockitoBean
    private Brain brain; // Mocking AI dependency for context load

    @Autowired

    private AgentRepository agentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PortalRepository portalRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    @DisplayName("Should teleport agent when movement finishes at portal location")
    void shouldTeleportAgentWhenMovementFinishesAtPortalLocation() {
        // Given: Two locations and a portal
        Location forest = locationRepository.save(Location.builder()
                .name("Integration Forest").width(100).height(100).type(LocationType.FOREST).build());
        Location meadow = locationRepository.save(Location.builder()
                .name("Integration Meadow").width(100).height(100).type(LocationType.FOREST).build());

        portalRepository.save(Portal.builder()
                .sourceLocation(forest).sourceX(10).sourceY(10)
                .targetLocation(meadow).targetX(5).targetY(5)
                .build());

        // Given: A player and an agent in the forest
        Player player = playerRepository.save(Player.create("TestUser", "password"));
        Agent agent = agentRepository.save(Agent.create(
                "PortalTester",
                player,
                forest,
                0, 0,
                1 // speed
        ));

        // Given: A WorldState representing the arrival at (10, 10)
        AgentWorldState arrivalState = AgentWorldState.builder()
                .agentId(agent.getId())
                .x(10).y(10)
                .status(AgentStatus.MOVING)
                .build();

        // When: Finalizing movement (this should publish AgentArrivedEvent)
        agentPersistenceService.finalizeMovement(arrivalState);

        // Then: The agent should be teleported and state updated in Postgres
        Agent updatedAgent = agentRepository.findById(agent.getId()).orElseThrow();
        assertEquals("Integration Meadow", updatedAgent.getCurrentLocation().getName());
        assertEquals(5, updatedAgent.getX());
        assertEquals(5, updatedAgent.getY());
        assertEquals(AgentStatus.IDLE, updatedAgent.getStatus());
    }
}
