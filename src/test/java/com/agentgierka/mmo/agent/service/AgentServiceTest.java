package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.world.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentService Unit Tests")
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentWorldStateRepository agentWorldStateRepository;

    @Mock
    private com.agentgierka.mmo.ai.service.AgentThinkingService agentThinkingService;

    @InjectMocks
    private AgentService agentService;

    @Test
    @DisplayName("Should assign goal and trigger thinking")
    void shouldAssignGoalAndTriggerThinking() {
        // Given
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.builder().id(agentId).name("Commander").build();
        String goal = "Go to the safe zone";

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        // When
        agentService.assignGoal(agentId, goal);

        // Then
        verify(agentRepository).save(argThat(a -> goal.equals(a.getGoal())));
        verify(agentThinkingService).processThinking(agentId);
    }

    @Test
    @DisplayName("Should return Postgres data when agent is not moving in Redis")
    void shouldReturnPostgresDataWhenAgentIsNotMovingInRedis() {
        // Given
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.builder()
                .id(agentId)
                .x(50).y(50)
                .status(AgentStatus.IDLE)
                .build();

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentWorldStateRepository.findById(agentId)).thenReturn(null);

        // When
        Agent result = agentService.findById(agentId);

        // Then
        assertEquals(50, result.getX());
        assertEquals(AgentStatus.IDLE, result.getStatus());
        verify(agentWorldStateRepository).findById(agentId);
    }

    @Test
    @DisplayName("Should override with Redis data when agent is moving")
    void shouldOverrideWithRedisDataWhenAgentIsMoving() {
        // Given
        UUID agentId = UUID.randomUUID();
        Agent postgresAgent = Agent.builder()
                .id(agentId)
                .x(50).y(50)
                .status(AgentStatus.IDLE)
                .build();

        AgentWorldState redisState = AgentWorldState.builder()
                .agentId(agentId)
                .x(55).y(55)
                .status(AgentStatus.MOVING)
                .build();

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(postgresAgent));
        when(agentWorldStateRepository.findById(agentId)).thenReturn(redisState);

        // When
        Agent result = agentService.findById(agentId);

        // Then
        assertEquals(55, result.getX());
        assertEquals(55, result.getY());
        assertEquals(AgentStatus.MOVING, result.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when move target is out of bounds")
    void shouldThrowExceptionWhenMoveTargetIsOutOfBounds() {
        // Given
        UUID agentId = UUID.randomUUID();
        Location forest = Location.builder().width(100).height(100).build();
        Agent agent = Agent.builder()
                .id(agentId)
                .currentLocation(forest)
                .build();

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            agentService.moveTo(agentId, 150, 50)
        );
    }

    @Test
    @DisplayName("Should update both repositories when movement starts")
    void shouldUpdateBothRepositoriesWhenMovementStarts() {
        // Given
        UUID agentId = UUID.randomUUID();
        Location forest = Location.builder().width(100).height(100).build();
        Agent agent = Agent.builder()
                .id(agentId)
                .currentLocation(forest)
                .x(50).y(50)
                .speed(5)
                .build();

        try (var mockedSync = mockStatic(TransactionSynchronizationManager.class)) {
            mockedSync.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            
            when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any(Agent.class))).thenReturn(agent);

            // When
            agentService.moveTo(agentId, 60, 60);

            // Then
            verify(agentRepository).save(argThat(a -> 
                a.getStatus() == AgentStatus.MOVING && a.getTargetX() == 60
            ));

            // Capture and execute the synchronization logic
            var captor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            mockedSync.verify(() -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));
            captor.getValue().afterCommit();

            verify(agentWorldStateRepository).save(argThat(ws -> 
                ws.getAgentId().equals(agentId) && ws.getTargetX() == 60
            ));
        }
    }

    @Test
    @DisplayName("Should update status in Postgres when requested")
    void shouldUpdateStatusInPostgresWhenRequested() {
        // Given
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.builder().id(agentId).build();

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        // When
        agentService.updateStatus(agentId, AgentStatus.RESTING, "Taking a break");

        // Then
        verify(agentRepository).save(argThat(a -> 
            a.getStatus() == AgentStatus.RESTING && 
            "Taking a break".equals(a.getCurrentActionDescription())
        ));
    }
}
