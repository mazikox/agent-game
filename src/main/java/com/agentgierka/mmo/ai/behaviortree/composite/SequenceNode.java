package com.agentgierka.mmo.ai.behaviortree.composite;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;

import java.util.List;
import java.util.stream.Collectors;

public class SequenceNode implements BehaviorNode {
    private final List<BehaviorNode> children;
    private int currentIndex = 0;

    public SequenceNode(List<BehaviorNode> children) {
        this.children = children;
    }

    @Override
    public NodeStatus tick(BehaviorContext context) {
        while (currentIndex < children.size()) {
            NodeStatus status = children.get(currentIndex).tick(context);
            if (status != NodeStatus.SUCCESS) {
                return status;
            }
            currentIndex++;
        }
        currentIndex = 0;
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        currentIndex = 0;
        for (BehaviorNode child : children) {
            child.reset();
        }
    }

    @Override
    public String describe() {
        return "Sequence[" + children.stream()
                .map(BehaviorNode::describe)
                .collect(Collectors.joining(", ")) + "]";
    }
}
