package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KillCountCondition implements GoalCondition {
    private final int targetKills;

    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        return progress.getKillCount() >= targetKills;
    }
}
