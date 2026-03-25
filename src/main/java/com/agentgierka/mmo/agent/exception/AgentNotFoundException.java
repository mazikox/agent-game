package com.agentgierka.mmo.agent.exception;

import com.agentgierka.mmo.exception.GameBaseException;

/**
 * Thrown when an agent is not found in the persistence layer.
 */
public class AgentNotFoundException extends GameBaseException {
    public AgentNotFoundException(String id) {
        super("Agent with ID " + id + " not found.", "AGENT_NOT_FOUND");
    }
}
