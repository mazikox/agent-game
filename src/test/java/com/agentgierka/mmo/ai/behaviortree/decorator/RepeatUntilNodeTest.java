package com.agentgierka.mmo.ai.behaviortree.decorator;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agentgierka.mmo.ai.behaviortree.condition.GoalCondition;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgress;
import com.agentgierka.mmo.agent.model.Agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepeatUntilNode Unit Tests")
class RepeatUntilNodeTest {

    @Mock
    private BehaviorNode child;

    @Mock
    private GoalCondition condition;
    @Mock
    private GoalProgress progress;
    @Mock
    private Agent agent;
    @Mock
    private BehaviorContext context;

    @Test
    @DisplayName("Should return SUCCESS without ticking child if condition is met")
    void shouldReturnSuccessIfConditionMet() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(context.goalProgress()).thenReturn(progress);
        when(context.agent()).thenReturn(agent);
        when(condition.isSatisfied(progress, agent)).thenReturn(true);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.SUCCESS, result);
        verifyNoInteractions(child);
    }

    @Test
    @DisplayName("Should return RUNNING if child succeeds but condition is not met")
    void shouldReturnRunningIfChildSucceeds() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(context.goalProgress()).thenReturn(progress);
        when(context.agent()).thenReturn(agent);
        when(condition.isSatisfied(progress, agent)).thenReturn(false);
        when(child.tick(context)).thenReturn(NodeStatus.SUCCESS);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.RUNNING, result);
        verify(child).tick(context);
    }

    @Test
    @DisplayName("Should return RUNNING (retry) if child fails (before max retries)")
    void shouldReturnRunningIfChildFails() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(context.goalProgress()).thenReturn(progress);
        when(context.agent()).thenReturn(agent);
        when(condition.isSatisfied(progress, agent)).thenReturn(false);
        when(child.tick(context)).thenReturn(NodeStatus.FAILURE);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.RUNNING, result, "Should return RUNNING for retry");
        verify(child).tick(context);
    }

    @Test
    @DisplayName("Should return RUNNING if child is running")
    void shouldReturnRunningIfChildIsRunning() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(context.goalProgress()).thenReturn(progress);
        when(context.agent()).thenReturn(agent);
        when(condition.isSatisfied(progress, agent)).thenReturn(false);
        when(child.tick(context)).thenReturn(NodeStatus.RUNNING);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.RUNNING, result);
        verify(child).tick(context);
    }
}
