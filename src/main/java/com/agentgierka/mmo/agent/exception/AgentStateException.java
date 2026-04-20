package com.agentgierka.mmo.agent.exception;

import com.agentgierka.mmo.exception.GameBaseException;

/**
 * Exception thrown when an agent's current state (e.g., IN_COMBAT) blocks a requested action.
 */
public class AgentStateException extends GameBaseException {
    public AgentStateException(String message) {
        super(message, "AGENT_STATE_CONFLICT");
    }
}
