package com.agentgierka.mmo.ai.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
import com.agentgierka.mmo.world.PortalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentThinkingService Unit Tests")
class AgentThinkingServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private PortalRepository portalRepository;

    @Mock
    private AgentWorldStateRepository agentWorldStateRepository;

    @Mock
    private Brain brain;

    @InjectMocks
    private AgentThinkingService agentThinkingService;

    @Test
    @DisplayName("Should coordinate thinking process and trigger Redis sync when moving")
    void shouldCoordinateThinkingProcessAndTriggerRedisSyncWhenMoving() {
        // Given
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.builder()
                .id(agentId)
                .name("Thinker")
                .status(AgentStatus.IDLE)
                .build();

        Thought thought = Thought.builder()
                .targetX(100).targetY(200)
                .status("MOVING")
                .actionSummary("Moving to target")
                .build();

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(portalRepository.findAllBySourceLocation(any())).thenReturn(List.of());
        when(brain.think(any(Perception.class))).thenReturn(thought);

        try (var mockedSync = mockStatic(TransactionSynchronizationManager.class)) {
            mockedSync.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            // When
            agentThinkingService.processThinking(agentId);

            // Then
            verify(agentRepository).save(argThat(a -> 
                a.getStatus() == AgentStatus.MOVING && a.getTargetX() == 100
            ));

            // Verify Redis Sync
            var captor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            mockedSync.verify(() -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));
            captor.getValue().afterCommit();

            verify(agentWorldStateRepository).save(argThat(ws -> 
                ws.getAgentId().equals(agentId) && ws.getTargetX() == 100
            ));
        }
    }
}
