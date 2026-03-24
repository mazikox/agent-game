package com.agentgierka.mmo.config;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.LocationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with initial game data during startup.
 * Focuses on providing a base state for development and testing.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final AgentRepository agentRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing game data...");

        // 1. Create a default Location
        Location forest = Location.builder()
                .name("Forest of Beginnings")
                .description("A lush green forest where young agents learn basic survival skills.")
                .type(LocationType.FOREST)
                .width(100)
                .height(100)
                .build();
        locationRepository.save(forest);

        // 2. Create a default Master Player
        Player master = Player.builder()
                .username("MasterAdmin")
                .gold(1000L)
                .charisma(10)
                .build();
        playerRepository.save(master);

        // 3. Create a starting Agent
        Agent scout = Agent.builder()
                .name("Shadow-01")
                .owner(master)
                .currentLocation(forest)
                .x(50)
                .y(50)
                .strength(5)
                .dexterity(8)
                .status(AgentStatus.IDLE)
                .currentActionDescription("Standing at the forest entrance, waiting for orders.")
                .build();
        agentRepository.save(scout);

        log.info("Game data initialization complete (v1).");
    }
}
