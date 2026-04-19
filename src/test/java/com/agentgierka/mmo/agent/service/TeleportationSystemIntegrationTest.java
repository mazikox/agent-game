package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
import com.agentgierka.mmo.agent.event.AgentGoalCompletedEvent;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.Portal;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.repository.LootTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("World Trigger System Integration Test")
class TeleportationSystemIntegrationTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AgentPersistenceService agentPersistenceService;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PortalRepository portalRepository;

    @MockitoBean
    private AgentWorldStateRepository agentWorldStateRepository;

    @MockitoBean
    private CreatureInstanceRepository creatureInstanceRepository;

    @Autowired
    private LootTableRepository lootTableRepository;

    @Autowired
    private TestEventCollector eventCollector;

    private UUID agentId;
    private Location forest;
    private Location mine;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEventCollector testEventCollector() {
            return new TestEventCollector();
        }
    }

    static class TestEventCollector {
        private final List<Object> events = new ArrayList<>();

        @EventListener
        public void onEvent(Object event) {
            events.add(event);
        }

        public void clear() {
            events.clear();
        }

        public <T> List<T> getEventsOfType(Class<T> type) {
            return events.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .toList();
        }
    }

    @BeforeEach
    void setUp() {
        tearDown(); // Clean start
        eventCollector.clear();

        // 1. Create Locations
        forest = Location.builder().name("Forest").width(100).height(100).build();
        mine = Location.builder().name("Mine").width(100).height(100).build();
        locationRepository.saveAll(List.of(forest, mine));

        // 2. Create Portal: Forest(90,90) -> Mine(6,6)
        Portal portal = Portal.builder()
                .sourceLocation(forest)
                .sourceX(90).sourceY(90)
                .targetLocation(mine)
                .targetX(6).targetY(6)
                .build();
        portalRepository.save(portal);

        // 3. Create Player & Agent
        Player player = Player.create("testUser", "pass");
        playerRepository.save(player);
        Agent agent = Agent.create("Teleportee", player, forest, 89, 89, 1);
        agentId = agentRepository.save(agent).getId();

        agentRepository.flush();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        lootTableRepository.deleteAll();
        portalRepository.deleteAll();
        agentRepository.deleteAll();
        playerRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should trigger teleportation and skip goal completion when stepping on a portal")
    void shouldTeleportAndSkipGoalEvent() {
        // Arrange
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .x(90).y(90)
                .build();

        // Act - Perform movement inside a transaction to trigger afterCommit
        transactionTemplate.execute(status -> {
            agentPersistenceService.finalizeMovement(state);
            return null;
        });

        // Assert - Part 1: Final database state (wrapped in transaction to avoid LazyInitException)
        transactionTemplate.execute(status -> {
            Agent updatedAgent = agentRepository.findById(agentId).orElseThrow();
            assertThat(updatedAgent.getCurrentLocation().getName()).isEqualTo("Mine");
            assertThat(updatedAgent.getX()).isEqualTo(6);
            assertThat(updatedAgent.getY()).isEqualTo(6);
            return null;
        });

        // Assert - Part 2: Events (verified outside Act transaction since they are published afterCommit)
        List<AgentGoalCompletedEvent> goalEvents = eventCollector.getEventsOfType(AgentGoalCompletedEvent.class);
        List<AgentArrivedEvent> arrivalEvents = eventCollector.getEventsOfType(AgentArrivedEvent.class);

        assertThat(goalEvents).as("Should NOT publish GoalCompletedEvent on portal").isEmpty();
        assertThat(arrivalEvents).as("Should publish exactly one arrival event").hasSize(1);
        assertThat(arrivalEvents.get(0).location().getId()).isEqualTo(mine.getId());
        assertThat(arrivalEvents.get(0).type().name()).isEqualTo("TELEPORT");
    }

    @Test
    @DisplayName("Should publish goal completion when NOT stepping on a portal")
    void shouldPublishGoalEventNormally() {
        // Arrange
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .x(10).y(10)
                .build();

        // Act
        transactionTemplate.execute(status -> {
            agentPersistenceService.finalizeMovement(state);
            return null;
        });

        // Assert
        transactionTemplate.execute(status -> {
            Agent updatedAgent = agentRepository.findById(agentId).orElseThrow();
            assertThat(updatedAgent.getCurrentLocation().getName()).isEqualTo("Forest");
            assertThat(updatedAgent.getX()).isEqualTo(10);
            assertThat(updatedAgent.getY()).isEqualTo(10);

            List<AgentGoalCompletedEvent> goalEvents = eventCollector.getEventsOfType(AgentGoalCompletedEvent.class);
            assertThat(goalEvents).hasSize(1);
            assertThat(goalEvents.get(0).location().getId()).isEqualTo(forest.getId());
            return null;
        });
    }
}
