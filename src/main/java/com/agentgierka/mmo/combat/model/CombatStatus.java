package com.agentgierka.mmo.combat.model;

/**
 * Defines the current state of a combat encounter.
 */
public enum CombatStatus {
    /** Combat is currently active and turns are being processed. */
    ONGOING,
    /** Combat has ended (someone died or fled). */
    FINISHED,
    /** Combat was interrupted (e.g. timeout or technical issue). */
    ABANDONED
}
