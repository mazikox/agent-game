package com.agentgierka.mmo.ai.stub;

import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.leaf.IdleAction;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.port.GoalPlanner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("test")
public class StubGoalPlanner implements GoalPlanner {
    @Override
    public Optional<BehaviorNode> planGoal(String goal, Perception perception) {
        return Optional.of(new IdleAction());
    }
}
