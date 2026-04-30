package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;

public class IdleAction implements BehaviorNode {

    @Override
    public NodeStatus tick(BehaviorContext context) {
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
    }

    @Override
    public String describe() {
        return "Idle";
    }
}
