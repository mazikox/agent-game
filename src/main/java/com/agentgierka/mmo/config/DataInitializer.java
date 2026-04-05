package com.agentgierka.mmo.config;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.LocationType;
import com.agentgierka.mmo.world.Portal;
import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PortalRepository portalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (locationRepository.count() > 0) {
            log.info("Game data already initialized. Skipping...");
            return;
        }

        log.info("Initializing game data...");

        // TODO: Use Flyway for future database schema and data migrations

        // 1. Create a default Location
        Location forest = Location.builder()
                .name("Forest of Beginnings")
                .description("A lush green forest where young agents learn basic survival skills.")
                .type(LocationType.FOREST)
                .width(100)
                .height(100)
                .build();
        locationRepository.save(forest);

        // 2. Create another Location for teleportation
        Location meadow = Location.builder()
                .name("Azure Meadow")
                .description("A peaceful meadow filled with blue flowers and butterflies.")
                .type(LocationType.FOREST)
                .width(50)
                .height(50)
                .build();
        locationRepository.save(meadow);

        // 3. Create a Portal from Forest(10,10) to Meadow(5,5)
        Portal forestToMeadow = Portal.builder()
                .sourceLocation(forest)
                .sourceX(10)
                .sourceY(10)
                .targetLocation(meadow)
                .targetX(5)
                .targetY(5)
                .build();
        portalRepository.save(forestToMeadow);

        // 4. Create a default Master Player
        Player master = Player.create("MasterAdmin", passwordEncoder.encode("admin123"));
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
                .stats(com.agentgierka.mmo.agent.model.AgentStats.createInitial())
                .status(AgentStatus.IDLE)
                .currentActionDescription("Standing at the forest entrance, waiting for orders.")
                .build();
        agentRepository.save(scout);

        log.info("Game data initialization complete (v1).");
    }
}
