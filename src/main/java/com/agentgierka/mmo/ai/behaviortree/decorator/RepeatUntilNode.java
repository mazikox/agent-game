package com.agentgierka.mmo.ai.behaviortree.decorator;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;

import java.util.function.Predicate;

public class RepeatUntilNode implements BehaviorNode {
    private final Predicate<BehaviorContext> condition;
    private final BehaviorNode child;

    public RepeatUntilNode(Predicate<BehaviorContext> condition, BehaviorNode child) {
        this.condition = condition;
        this.child = child;
    }

    @Override
    public NodeStatus tick(BehaviorContext context) {
        if (condition.test(context)) {
            return NodeStatus.SUCCESS;
        }
        
        NodeStatus status = child.tick(context);
        
        if (status == NodeStatus.FAILURE) {
            return NodeStatus.FAILURE;
        }
        
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        child.reset();
    }

    @Override
    public String describe() {
        return "RepeatUntil(" + child.describe() + ")";
    }
}
