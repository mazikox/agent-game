package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExpGainedCondition implements GoalCondition {
    private final int targetExp;
    private final ComparisonOperator operator;

    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        return operator.compare(progress.getExpGained(), targetExp);
    }
}
