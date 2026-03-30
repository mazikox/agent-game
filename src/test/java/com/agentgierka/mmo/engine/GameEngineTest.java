package com.agentgierka.mmo.engine;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameEngine Unit Tests")
class GameEngineTest {

    @Mock
    private AgentWorldStateRepository agentWorldStateRepository;

    @Mock
    private AgentPersistenceService agentPersistenceService;

    @InjectMocks
    private GameEngine gameEngine;

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
        verify(agentWorldStateRepository).saveAll(anyList());
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
        verify(agentWorldStateRepository).saveAll(anyList());
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
        verify(agentWorldStateRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should not process anything when no agents are active")
    void shouldNotProcessAnythingWhenNoAgentsAreActive() {
        // Given
        when(agentWorldStateRepository.findAllActive()).thenReturn(List.of());

        // When
        gameEngine.tick();

        // Then
        verify(agentWorldStateRepository, never()).saveAll(anyList());
        verifyNoInteractions(agentPersistenceService);
    }
}
