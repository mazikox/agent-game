package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StateSyncIntegrationTest {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentWorldStateRepository redisRepository;

    @Autowired
    private AgentWorldStateRecoveryService recoveryService;

    @Autowired
    private AgentWorldStateCheckpointService checkpointService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private com.agentgierka.mmo.world.PortalRepository portalRepository;

    private Agent testAgent;
    private Location testLocation;
    private Player testOwner;

    @BeforeEach
    void setUp() {
        redisRepository.deleteAll();
        agentRepository.deleteAll();
        portalRepository.deleteAll();
        locationRepository.deleteAll();
        playerRepository.deleteAll();

        testLocation = locationRepository.save(Location.builder().name("Test City").width(100).height(100).build());
        testOwner = playerRepository.save(Player.create("test_user", "password"));
        
        Agent agent = Agent.create("Test Agent", testOwner, testLocation, 10, 10, 5);
        testAgent = agentRepository.save(agent);
    }

    @Test
    void shouldRecoverMovingAgentOnStartup() {
        // Given: Agent is MOVING in Postgres but Redis is empty
        Agent agent = agentRepository.findById(testAgent.getId()).orElseThrow();
        agent.startMovement(50, 50, "Moving to center");
        agentRepository.save(agent);
        redisRepository.deleteAll();

        // When: Recovery runs
        recoveryService.recoverStatesOnStartup();

        // Then: Redis should have the state
        AgentWorldState recovered = redisRepository.findById(testAgent.getId());
        assertThat(recovered).isNotNull();
        assertThat(recovered.getAgentId()).isEqualTo(testAgent.getId());
        assertThat(recovered.getStatus()).isEqualTo(AgentStatus.MOVING);
        assertThat(recovered.getTargetX()).isEqualTo(50);
        assertThat(recovered.getTargetY()).isEqualTo(50);
    }

    @Test
    void shouldCheckpointRedisStateToPostgres() {
        // Given: Agent is moving in Redis
        AgentWorldState state = AgentWorldState.fromAgent(testAgent);
        state = state.toBuilder()
                .status(AgentStatus.MOVING)
                .x(25).y(25) // Current temporary position in Redis
                .targetX(50).targetY(50)
                .build();
        redisRepository.save(state);

        // When: Checkpoint runs
        checkpointService.performCheckpoint();

        // Then: Postgres should have updated coordinates
        Agent persisted = agentRepository.findById(testAgent.getId()).orElseThrow();
        assertThat(persisted.getX()).isEqualTo(25);
        assertThat(persisted.getY()).isEqualTo(25);
    }
}
