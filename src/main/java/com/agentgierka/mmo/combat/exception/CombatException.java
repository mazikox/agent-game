package com.agentgierka.mmo.combat.exception;

import com.agentgierka.mmo.exception.GameBaseException;

/**
 * Exception thrown when a combat-related rule is violated.
 */
public class CombatException extends GameBaseException {
    public CombatException(String message) {
        super(message, "COMBAT_ERROR");
    }
}
