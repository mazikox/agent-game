package com.agentgierka.mmo.creature.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Defines the rarity and difficulty of a creature.
 * Influences stats, loot quality, and respawn logic.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum CreatureRank {
    NORMAL,
    ELITE,
    RARE,
    BOSS
}
