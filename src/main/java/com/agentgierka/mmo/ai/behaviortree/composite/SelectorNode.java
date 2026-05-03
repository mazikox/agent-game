package com.agentgierka.mmo.ai.behaviortree.composite;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;

import java.util.List;
import java.util.stream.Collectors;

public class SelectorNode implements BehaviorNode {
    private final List<BehaviorNode> children;
    private int currentIndex = 0;

    public SelectorNode(List<BehaviorNode> children) {
        this.children = children != null ? 
            children.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList()) : 
            List.of();
    }

    @Override
    public NodeStatus tick(BehaviorContext context) {
        if (children.isEmpty()) return NodeStatus.FAILURE;
        while (currentIndex < children.size()) {
            NodeStatus status = children.get(currentIndex).tick(context);
            if (status == NodeStatus.SUCCESS) {
                currentIndex = 0;
                return NodeStatus.SUCCESS;
            }
            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }
            currentIndex++;
        }
        currentIndex = 0;
        return NodeStatus.FAILURE;
    }

    @Override
    public void reset() {
        currentIndex = 0;
        children.forEach(BehaviorNode::reset);
    }

    @Override
    public String describe() {
        return "Selector[" + children.stream()
                .map(n -> n != null ? n.describe() : "null")
                .collect(Collectors.joining(", ")) + "]";
    }
}
