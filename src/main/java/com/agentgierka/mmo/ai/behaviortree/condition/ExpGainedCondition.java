package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExpGainedCondition implements GoalCondition {
    private final int targetExp;

    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        return progress.getExpGained() >= targetExp;
    }
}
