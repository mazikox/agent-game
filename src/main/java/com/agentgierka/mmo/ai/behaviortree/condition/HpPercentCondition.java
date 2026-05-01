package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HpPercentCondition implements GoalCondition {
    private final int thresholdPercent;

    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        if (agent.getStats() == null || agent.getStats().getMaxHp() <= 0) {
            return false;
        }
        int currentPercent = (agent.getStats().getHp() * 100) / agent.getStats().getMaxHp();
        return currentPercent <= thresholdPercent;
    }
}
