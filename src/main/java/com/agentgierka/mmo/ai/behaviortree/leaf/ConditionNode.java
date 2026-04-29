package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;

import java.util.function.Predicate;

public class ConditionNode implements BehaviorNode {
    private final Predicate<BehaviorContext> predicate;
    private final String description;

    public ConditionNode(Predicate<BehaviorContext> predicate, String description) {
        this.predicate = predicate;
        this.description = description;
    }

    @Override
    public NodeStatus tick(BehaviorContext context) {
        return predicate.test(context) ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }

    @Override
    public void reset() {
        // Conditions hold no internal state
    }

    @Override
    public String describe() {
        return "Condition[" + description + "]";
    }
}
