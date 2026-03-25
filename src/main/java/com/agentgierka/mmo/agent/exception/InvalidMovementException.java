package com.agentgierka.mmo.agent.exception;

import com.agentgierka.mmo.exception.GameBaseException;

/**
 * Thrown when a movement request is invalid (e.g., out of bounds).
 */
public class InvalidMovementException extends GameBaseException {
    public InvalidMovementException(String message) {
        super(message, "INVALID_MOVEMENT");
    }
}
