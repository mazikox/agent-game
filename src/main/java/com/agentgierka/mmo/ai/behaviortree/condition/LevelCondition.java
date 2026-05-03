package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LevelCondition implements GoalCondition {
    private final int targetLevel;
    private final ComparisonOperator operator;

    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        return agent.getStats() != null && operator.compare(agent.getStats().getLevel(), targetLevel);
    }
}
