package com.agentgierka.mmo.agent.model;

/**
 * Defines the nature of an agent's movement or arrival at a location.
 * Used to control event-driven logic (e.g., portal triggers).
 */
public enum MovementType {
    /** Regular step-by-step movement processed by the game engine. */
    NORMAL,
    
    /** Instantaneous movement triggered by a portal. */
    PORTAL,
    
    /** Forced movement (e.g., GM command, respawn). */
    TELEPORT,
    
    /** Unknown or system-forced movement. */
    SYSTEM
}
