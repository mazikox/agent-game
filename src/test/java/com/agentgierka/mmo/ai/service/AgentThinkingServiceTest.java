package com.agentgierka.mmo.ai.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.agentgierka.mmo.agent.service.ActionResolverService;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
import com.agentgierka.mmo.ai.model.ActionType;
import com.agentgierka.mmo.ai.model.Decision;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

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
    private CreatureInstanceRepository creatureInstanceRepository;

    @Mock
    private WorldStateSynchronizer worldStateSynchronizer;

    @Mock
    private Brain brain;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ActionResolverService actionResolverService;

    @InjectMocks
    private AgentThinkingService agentThinkingService;

    @Test
    @DisplayName("Should coordinate thinking process and trigger Redis sync when moving")
    void shouldCoordinateThinkingProcessAndTriggerRedisSyncWhenMoving() {
        // Given
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.create("Thinker", null, 
                Location.builder().name("Test Location").build(), 
                0, 0, 1);
        ReflectionTestUtils.setField(agent, "id", agentId);

        Thought thought = Thought.builder()
                .actions(List.of(Decision.builder()
                        .actionType("MOVE_TO_CREATURE")
                        .actionSummary("Moving to target")
                        .build()))
                .build();

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(portalRepository.findAllBySourceLocationId(any())).thenReturn(List.of());
        when(creatureInstanceRepository.findAllByLocationId(any())).thenReturn(List.of());
        when(brain.think(any(Perception.class))).thenReturn(thought);
        when(actionResolverService.resolve(any(), any())).thenReturn(
                new ActionResolverService.ResolvedTarget(100, 200, AgentStatus.MOVING)
        );

        // When
        agentThinkingService.processThinking(agentId);

        // Then
        verify(agentRepository).save(argThat(a -> 
            a.getStatus() == AgentStatus.MOVING && a.getTargetX() == 100
        ));
        verify(worldStateSynchronizer).syncMovementAfterCommit(any(Agent.class));
    }
}
