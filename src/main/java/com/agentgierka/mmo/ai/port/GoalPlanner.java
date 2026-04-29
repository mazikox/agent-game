package com.agentgierka.mmo.ai.port;

import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.model.Perception;

import java.util.Optional;

public interface GoalPlanner {
    Optional<BehaviorNode> planGoal(String goal, Perception perception);
}
