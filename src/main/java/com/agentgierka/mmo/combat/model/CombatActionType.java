package com.agentgierka.mmo.combat.model;

/**
 * Defines available actions an Agent can perform during a combat turn.
 */
public enum CombatActionType {
    /** Basic physical attack. Consumes 100 AP. */
    ATTACK,
    /** Use a special ability. Consumes 100 AP (placeholder). */
    SKILL,
    /** Consume a healing potion. Does NOT consume a turn (AP). */
    POTION,
    /** Attempt to escape from the battle. Ends combat if successful. */
    FLEE
}
