package com.agentgierka.mmo.world;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import com.agentgierka.mmo.agent.service.AgentService;
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
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Portal Chain Integration Tests")
class PortalChainIntegrationTest {

    @Autowired
    private AgentPersistenceService agentPersistenceService;

    @MockitoBean
    private AgentWorldStateRepository agentWorldStateRepository;

    @MockitoBean
    private Brain brain;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PortalRepository portalRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AgentService agentService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @Transactional
    @DisplayName("Should stop teleportation after first jump in a portal chain")
    void shouldStopAfterFirstJump() {
        // Given
        Location forest = saveLocation("Forest", 100, 100);
        Location meadow = saveLocation("Meadow", 100, 100);
        Location desert = saveLocation("Desert", 100, 100);

        savePortal(forest, 10, 10, meadow, 5, 5);
        savePortal(meadow, 5, 5, desert, 1, 1);

        Agent agent = saveAgent("Chainer", forest, 0, 0);
        AgentWorldState arrivalAtA = AgentWorldState.builder().agentId(agent.getId()).x(10).y(10).status(AgentStatus.MOVING).build();

        // When
        agentPersistenceService.finalizeMovement(arrivalAtA);

        // Then
        Agent updatedAgent = agentRepository.findById(agent.getId()).orElseThrow();
        assertEquals("Meadow", updatedAgent.getCurrentLocation().getName());
        assertEquals(5, updatedAgent.getX());
    }

    @Test
    @Transactional
    @DisplayName("Should prevent infinite loops in circular portal references")
    void shouldPreventCircularPortalLoops() {
        // Given
        Location loc1 = saveLocation("Loc1", 100, 100);
        Location loc2 = saveLocation("Loc2", 100, 100);

        savePortal(loc1, 1, 1, loc2, 2, 2);
        savePortal(loc2, 2, 2, loc1, 1, 1);

        Agent agent = saveAgent("Circler", loc1, 0, 0);
        AgentWorldState arrivalAtA = AgentWorldState.builder().agentId(agent.getId()).x(1).y(1).status(AgentStatus.MOVING).build();

        // When
        agentPersistenceService.finalizeMovement(arrivalAtA);

        // Then
        Agent updatedAgent = agentRepository.findById(agent.getId()).orElseThrow();
        assertEquals("Loc2", updatedAgent.getCurrentLocation().getName());
    }

    @Test
    @DisplayName("Should clear Redis state after direct teleportation")
    void shouldClearRedisWhenTeleportingDirectly() {
        // Given
        Location forest = saveLocation("Forest3", 100, 100);
        Location meadow = saveLocation("Meadow3", 100, 100);
        Agent agent = saveAgent("DirectTester3", forest, 0, 0);

        // When
        transactionTemplate.execute(status -> {
            agentService.teleportTo(agent.getId(), meadow, 5, 5);
            return null;
        });

        // Then
        verify(agentWorldStateRepository, times(1)).delete(agent.getId());
    }

    private Location saveLocation(String name, int w, int h) {
        return transactionTemplate.execute(s -> locationRepository.save(Location.builder().name(name).width(w).height(h).type(LocationType.FOREST).build()));
    }

    private void savePortal(Location src, int sx, int sy, Location dst, int dx, int dy) {
        transactionTemplate.execute(s -> portalRepository.save(Portal.builder().sourceLocation(src).sourceX(sx).sourceY(sy).targetLocation(dst).targetX(dx).targetY(dy).build()));
    }

    private Agent saveAgent(String name, Location loc, int x, int y) {
        Player player = transactionTemplate.execute(s -> playerRepository.save(Player.create(name + "Player", "secret")));
        return transactionTemplate.execute(s -> agentRepository.save(Agent.create(name, player, loc, x, y, 1)));
    }
}
