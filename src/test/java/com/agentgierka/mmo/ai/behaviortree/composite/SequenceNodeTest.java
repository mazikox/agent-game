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
@DisplayName("SequenceNode Unit Tests")
class SequenceNodeTest {

    @Mock
    private BehaviorNode child1;

    @Mock
    private BehaviorNode child2;

    @Mock
    private BehaviorContext context;

    @Test
    @DisplayName("Should execute children sequentially and return RUNNING when a child is running")
    void shouldExecuteChildrenSequentiallyAndReturnRunning() {
        SequenceNode sequenceNode = new SequenceNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.SUCCESS);
        when(child2.tick(context)).thenReturn(NodeStatus.RUNNING);

        NodeStatus result = sequenceNode.tick(context);

        assertEquals(NodeStatus.RUNNING, result);
        verify(child1).tick(context);
        verify(child2).tick(context);
    }

    @Test
    @DisplayName("Should return FAILURE immediately if a child fails")
    void shouldReturnFailureIfChildFails() {
        SequenceNode sequenceNode = new SequenceNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.FAILURE);

        NodeStatus result = sequenceNode.tick(context);

        assertEquals(NodeStatus.FAILURE, result);
        verify(child1).tick(context);
        verifyNoInteractions(child2);
    }
    
    @Test
    @DisplayName("Should resume execution from the running child in the next tick")
    void shouldResumeFromRunningChild() {
        SequenceNode sequenceNode = new SequenceNode(List.of(child1, child2));
        when(child1.tick(context)).thenReturn(NodeStatus.SUCCESS);
        when(child2.tick(context)).thenReturn(NodeStatus.RUNNING);
        
        sequenceNode.tick(context);
        
        when(child2.tick(context)).thenReturn(NodeStatus.SUCCESS);
        
        NodeStatus result = sequenceNode.tick(context);
        
        assertEquals(NodeStatus.SUCCESS, result);
        verify(child1, times(1)).tick(context);
        verify(child2, times(2)).tick(context);
    }
}
