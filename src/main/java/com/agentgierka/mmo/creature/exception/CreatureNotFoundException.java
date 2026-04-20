package com.agentgierka.mmo.creature.exception;

import com.agentgierka.mmo.exception.GameBaseException;

/**
 * Exception thrown when a specific creature instance cannot be found in the repository.
 */
public class CreatureNotFoundException extends GameBaseException {
    public CreatureNotFoundException(String creatureId) {
        super("Creature not found: " + creatureId, "CREATURE_NOT_FOUND");
    }
}
