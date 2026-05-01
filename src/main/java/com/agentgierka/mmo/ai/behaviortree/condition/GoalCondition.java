package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;

/**
 * Strategy interface for checking if a goal condition is met.
 */
@FunctionalInterface
public interface GoalCondition {
    
    /**
     * Evaluates the condition based on goal progress and current agent state.
     */
    boolean isSatisfied(GoalProgress progress, Agent agent);

    default GoalCondition and(GoalCondition other) {
        return (p, a) -> this.isSatisfied(p, a) && other.isSatisfied(p, a);
    }

    default GoalCondition or(GoalCondition other) {
        return (p, a) -> this.isSatisfied(p, a) || other.isSatisfied(p, a);
    }
}
