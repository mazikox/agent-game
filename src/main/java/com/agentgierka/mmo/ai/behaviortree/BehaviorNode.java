package com.agentgierka.mmo.ai.behaviortree;

public interface BehaviorNode {
    NodeStatus tick(BehaviorContext context);
    void reset();
    String describe();
}
