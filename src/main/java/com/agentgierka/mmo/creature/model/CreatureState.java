package com.agentgierka.mmo.creature.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Defines the current state of a creature instance in the game world.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum CreatureState {
    ALIVE,
    DEAD,
    IN_COMBAT
}
