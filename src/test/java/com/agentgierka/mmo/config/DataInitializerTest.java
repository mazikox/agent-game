package com.agentgierka.mmo.config;

import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.creature.repository.*;
import com.agentgierka.mmo.creature.service.SpawnService;
import com.agentgierka.mmo.engine.EngineControl;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.PortalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private CreatureTemplateRepository creatureTemplateRepository;
    @Mock private AgentWorldStateRepository agentWorldStateRepository;
    @Mock private CreatureInstanceRepository creatureInstanceRepository;
    @Mock private SpawnService spawnService;
    @Mock private EngineControl engineControl;

    // Remaining mocks for constructor injection
    @Mock private AgentRepository agentRepository;
    @Mock private PortalRepository portalRepository;
    @Mock private SpawnPointRepository spawnPointRepository;
    @Mock private LootTableRepository lootTableRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void shouldSkipSeedingIfDataExistsButStillPopulateRedis() {
        // Given
        when(locationRepository.count()).thenReturn(1L);

        // When
        dataInitializer.run();

        // Then
        verify(agentWorldStateRepository).deleteAll();
        verify(creatureInstanceRepository).deleteAll();
        verify(locationRepository, never()).save(any());
        verify(creatureTemplateRepository, never()).saveAll(any());
        verify(spawnService).spawnAllActivePoints();
        verify(engineControl).setReady(true);
    }
}
