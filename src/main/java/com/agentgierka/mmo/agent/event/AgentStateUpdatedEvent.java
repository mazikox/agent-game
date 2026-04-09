package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.agent.model.AgentWorldState;
import lombok.Getter;

/**
 * Event published when an agent's world state (position, status) is updated.
 */
@Getter
public class AgentStateUpdatedEvent {
    private final AgentWorldState state;

    public AgentStateUpdatedEvent(AgentWorldState state) {
        this.state = state;
    }
}
