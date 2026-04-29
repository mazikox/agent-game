package com.agentgierka.mmo.ai.stub;

import com.agentgierka.mmo.ai.model.ActionType;
import com.agentgierka.mmo.ai.model.Decision;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("test")
public class StubBrain implements Brain {
    @Override
    public Thought think(Perception perception) {
        return Thought.builder()
                .actions(List.of(
                        Decision.builder()
                                .actionType("IDLE")
                                .actionSummary("Thinking simulated in tests.")
                                .build()
                ))
                .build();
    }
}
