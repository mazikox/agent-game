package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;

/**
 * Fallback condition that is never satisfied. 
 * Used for unknown or malformed conditions to prevent accidental goal completion.
 */
public class AlwaysFalseCondition implements GoalCondition {
    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        return false;
    }
}
