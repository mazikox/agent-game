package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocationReachedCondition implements GoalCondition {
    private final String locationName;

    @Override
    public boolean isSatisfied(GoalProgress progress, Agent agent) {
        return agent.getCurrentLocation() != null && 
               agent.getCurrentLocation().getName().equalsIgnoreCase(locationName);
    }
}
