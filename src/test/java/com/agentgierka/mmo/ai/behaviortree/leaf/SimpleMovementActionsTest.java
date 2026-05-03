package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.ai.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleMovementActionsTest {

    @Mock
    private BehaviorContext context;

    @Mock
    private Agent agent;

    @BeforeEach
    void setUp() {
        lenient().when(context.agent()).thenReturn(agent);
    }

    @Test
    void idleAction_alwaysReturnsSuccess() {
        IdleAction action = new IdleAction();
        NodeStatus status = action.tick(context);
        assertEquals(NodeStatus.SUCCESS, status);
    }

    @Test
    void moveToPositionAction_whenNotAtTarget_initiatesMovementAndReturnsRunning() {
        MoveToPositionAction action = new MoveToPositionAction(10, 20);

        when(agent.getX()).thenReturn(5);
        when(agent.getY()).thenReturn(5);
        when(agent.getStatus()).thenReturn(AgentStatus.IDLE);

        NodeStatus status = action.tick(context);

        assertEquals(NodeStatus.RUNNING, status);
        verify(agent).startMovement(eq(10), eq(20), anyString());
    }

    @Test
    void moveToPositionAction_whenAlreadyMovingToTarget_returnsRunningWithoutRestarting() {
        MoveToPositionAction action = new MoveToPositionAction(10, 20);

        when(agent.getX()).thenReturn(5);
        when(agent.getY()).thenReturn(5);
        when(agent.getStatus()).thenReturn(AgentStatus.MOVING);
        when(agent.getTargetX()).thenReturn(10);
        when(agent.getTargetY()).thenReturn(20);

        NodeStatus status = action.tick(context);

        assertEquals(NodeStatus.RUNNING, status);
        verify(agent, never()).startMovement(anyInt(), anyInt(), anyString());
    }

    @Test
    void moveToPositionAction_whenAtTarget_returnsSuccess() {
        MoveToPositionAction action = new MoveToPositionAction(10, 20);

        when(agent.getX()).thenReturn(10);
        when(agent.getY()).thenReturn(20);

        NodeStatus status = action.tick(context);

        assertEquals(NodeStatus.SUCCESS, status);
        verify(agent, never()).startMovement(anyInt(), anyInt(), anyString());
    }
}
