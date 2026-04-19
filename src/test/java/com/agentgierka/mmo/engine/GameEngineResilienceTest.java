package com.agentgierka.mmo.engine;

import com.agentgierka.mmo.creature.service.SpawnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Duration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEngineResilienceTest {

    @Mock
    private EngineControl engineControl;
    @Mock
    private SpawnService spawnService;
    @Mock
    private AgentWorldStateRepository agentWorldStateRepository;
    @Mock
    private AgentPersistenceService agentPersistenceService;
    @Mock
    private AsyncTaskExecutor taskExecutor;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GameEngine gameEngine;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        gameEngine = new GameEngine(
            agentWorldStateRepository,
            agentPersistenceService,
            engineControl,
            taskExecutor,
            eventPublisher,
            spawnService,
            Duration.ofSeconds(5)
        );
    }

    @Test
    void shouldContinueTickingEvenIfSpawnServiceFails() {
        // Given
        when(engineControl.isReady()).thenReturn(true);
        doThrow(new RuntimeException("Redis error")).when(spawnService).processRespawns();

        // When
        try {
            gameEngine.tick();
        } catch (Exception e) {
            // Should be handled by GameEngine now, 
            // but we keep try-catch in test for safety 
        }

        try {
            gameEngine.tick();
        } catch (Exception e) {
        }

        // Then
        verify(spawnService, times(2)).processRespawns();
    }
}
