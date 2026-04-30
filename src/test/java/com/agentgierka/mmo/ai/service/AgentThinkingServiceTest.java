package com.agentgierka.mmo.ai.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeExecutor;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeRegistry;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.port.GoalPlanner;
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GoalPlanner goalPlanner;

    @Mock
    private BehaviorTreeRegistry behaviorTreeRegistry;

    @Mock
    private BehaviorTreeExecutor behaviorTreeExecutor;

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
        agent.assignGoal("Test goal"); // set active goal

        BehaviorNode mockNode = mock(BehaviorNode.class);

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(portalRepository.findAllBySourceLocationId(any())).thenReturn(List.of());
        when(creatureInstanceRepository.findAllByLocationId(any())).thenReturn(List.of());
        when(behaviorTreeRegistry.get(agentId)).thenReturn(Optional.empty()); // No tree yet
        when(goalPlanner.planGoal(anyString(), any(Perception.class))).thenReturn(Optional.of(mockNode));
        
        doAnswer(inv -> {
            Agent a = inv.getArgument(0);
            a.startMovement(100, 200, "Moving via BT");
            return null;
        }).when(behaviorTreeExecutor).tick(any(Agent.class));

        // When
        agentThinkingService.processThinking(agentId);

        // Then
        verify(behaviorTreeRegistry).register(agentId, mockNode);
        verify(behaviorTreeExecutor).tick(agent);
        verify(agentRepository).save(argThat(a -> 
            a.getStatus() == AgentStatus.MOVING && a.getTargetX() == 100
        ));
        verify(worldStateSynchronizer).syncMovementAfterCommit(any(Agent.class));
    }
}
