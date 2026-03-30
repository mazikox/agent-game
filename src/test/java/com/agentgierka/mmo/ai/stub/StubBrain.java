package com.agentgierka.mmo.ai.stub;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class StubBrain implements Brain {
    @Override
    public Thought think(Perception perception) {
        return Thought.builder()
                .actionSummary("Thinking simulated in tests.")
                .nextGoal(perception.currentGoal())
                .targetX(perception.x())
                .targetY(perception.y())
                .status("IDLE")
                .build();
    }
}
