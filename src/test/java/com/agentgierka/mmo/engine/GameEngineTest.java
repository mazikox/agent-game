package com.agentgierka.mmo.engine;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameEngine Unit Tests")
class GameEngineTest {

    @Mock
    private AgentWorldStateRepository agentWorldStateRepository;

    @Mock
    private AgentPersistenceService agentPersistenceService;

    @Mock
    private EngineControl engineControl;

    @Mock
    private AsyncTaskExecutor taskExecutor;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        lenient().when(engineControl.isReady()).thenReturn(true);
        // Mock taskExecutor to run synchronously for unit tests to avoid race conditions
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            return CompletableFuture.runAsync(task);
        }).when(taskExecutor).execute(any(Runnable.class));

        gameEngine = new GameEngine(
                agentWorldStateRepository,
                agentPersistenceService,
                engineControl,
                taskExecutor,
                eventPublisher,
                Duration.ofSeconds(5)
        );
    }

    @Test
    @DisplayName("Should move agent one step closer in both axes on each tick")
    void shouldMoveAgentOneStepCloserInBothAxesOnEachTick() {
        // Given
        UUID agentId = UUID.randomUUID();
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .x(50).y(50)
                .targetX(60).targetY(60)
                .status(AgentStatus.MOVING)
                .build();

        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of(state));

        // When
        gameEngine.tick();

        // Then
        assertEquals(51, state.getX());
        assertEquals(51, state.getY());
        verify(agentWorldStateRepository).updateAtomic(state);
        verifyNoInteractions(agentPersistenceService);
    }

    @Test
    @DisplayName("Should move only in X axis when Y is already at target")
    void shouldMoveOnlyInXAxisWhenYIsAlreadyAtTarget() {
        // Given
        UUID agentId = UUID.randomUUID();
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .x(50).y(60)
                .targetX(60).targetY(60)
                .status(AgentStatus.MOVING)
                .build();

        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of(state));

        // When
        gameEngine.tick();

        // Then
        assertEquals(51, state.getX());
        assertEquals(60, state.getY());
        verify(agentWorldStateRepository).updateAtomic(state);
    }

    @Test
    @DisplayName("Should call persistence service when agent reaches destination")
    void shouldCallPersistenceServiceWhenAgentReachesDestination() {
        // Given
        UUID agentId = UUID.randomUUID();
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .x(59).y(60)
                .targetX(60).targetY(60)
                .status(AgentStatus.MOVING)
                .build();

        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of(state));

        // When
        gameEngine.tick();

        // Then
        assertEquals(60, state.getX());
        assertEquals(60, state.getY());
        verify(agentPersistenceService).finalizeMovement(state);
        // Should not save to Redis again if finalized
        verify(agentWorldStateRepository, never()).updateAtomic(any());
    }

    @Test
    @DisplayName("Should not process anything when no agents are active")
    void shouldNotProcessAnythingWhenNoAgentsAreActive() {
        // Given
        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of());

        // When
        gameEngine.tick();

        // Then
        verify(agentWorldStateRepository, never()).updateAtomic(any());
        verifyNoInteractions(agentPersistenceService);
    }
}
