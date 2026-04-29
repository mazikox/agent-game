package com.agentgierka.mmo.ai.behaviortree.composite;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SelectorNode Unit Tests")
class SelectorNodeTest {

    @Mock
    private BehaviorNode child1;

    @Mock
    private BehaviorNode child2;

    @Mock
    private BehaviorContext context;

    @Test
    @DisplayName("Should return SUCCESS immediately if first child succeeds (Short-circuit)")
    void shouldShortCircuitOnSuccess() {
        SelectorNode selectorNode = new SelectorNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.SUCCESS);

        NodeStatus result = selectorNode.tick(context);

        assertEquals(NodeStatus.SUCCESS, result);
        verify(child1).tick(context);
        verifyNoInteractions(child2);
    }

    @Test
    @DisplayName("Should try next child if first child fails")
    void shouldTryNextChildOnFailure() {
        SelectorNode selectorNode = new SelectorNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.FAILURE);
        when(child2.tick(context)).thenReturn(NodeStatus.RUNNING);

        NodeStatus result = selectorNode.tick(context);

        assertEquals(NodeStatus.RUNNING, result);
        verify(child1).tick(context);
        verify(child2).tick(context);
    }

    @Test
    @DisplayName("Should return FAILURE if all children fail")
    void shouldReturnFailureIfAllChildrenFail() {
        SelectorNode selectorNode = new SelectorNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.FAILURE);
        when(child2.tick(context)).thenReturn(NodeStatus.FAILURE);

        NodeStatus result = selectorNode.tick(context);

        assertEquals(NodeStatus.FAILURE, result);
        verify(child1).tick(context);
        verify(child2).tick(context);
    }

    @Test
    @DisplayName("Should resume execution from the running child in the next tick")
    void shouldResumeFromRunningChild() {
        SelectorNode selectorNode = new SelectorNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.FAILURE);
        when(child2.tick(context)).thenReturn(NodeStatus.RUNNING);

        selectorNode.tick(context);

        when(child2.tick(context)).thenReturn(NodeStatus.SUCCESS);

        NodeStatus result = selectorNode.tick(context);

        assertEquals(NodeStatus.SUCCESS, result);
        verify(child1, times(1)).tick(context);
        verify(child2, times(2)).tick(context);
    }
}
