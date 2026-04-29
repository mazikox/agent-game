package com.agentgierka.mmo.ai.behaviortree.decorator;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepeatUntilNode Unit Tests")
class RepeatUntilNodeTest {

    @Mock
    private BehaviorNode child;

    @Mock
    private Predicate<BehaviorContext> condition;

    @Mock
    private BehaviorContext context;

    @Test
    @DisplayName("Should return SUCCESS without ticking child if condition is met")
    void shouldReturnSuccessIfConditionMet() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(condition.test(context)).thenReturn(true);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.SUCCESS, result);
        verifyNoInteractions(child);
    }

    @Test
    @DisplayName("Should return RUNNING if child succeeds but condition is not met")
    void shouldReturnRunningIfChildSucceeds() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(condition.test(context)).thenReturn(false);
        when(child.tick(context)).thenReturn(NodeStatus.SUCCESS);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.RUNNING, result);
        verify(child).tick(context);
    }

    @Test
    @DisplayName("Should return FAILURE immediately if child fails")
    void shouldReturnFailureIfChildFails() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(condition.test(context)).thenReturn(false);
        when(child.tick(context)).thenReturn(NodeStatus.FAILURE);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.FAILURE, result);
        verify(child).tick(context);
    }

    @Test
    @DisplayName("Should return RUNNING if child is running")
    void shouldReturnRunningIfChildIsRunning() {
        RepeatUntilNode node = new RepeatUntilNode(condition, child);
        when(condition.test(context)).thenReturn(false);
        when(child.tick(context)).thenReturn(NodeStatus.RUNNING);

        NodeStatus result = node.tick(context);

        assertEquals(NodeStatus.RUNNING, result);
        verify(child).tick(context);
    }
}
