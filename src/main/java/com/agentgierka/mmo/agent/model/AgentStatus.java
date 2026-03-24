package com.agentgierka.mmo.agent.model;

/**
 * Defines the possible states an agent can be in within the game world.
 */
public enum AgentStatus {
    /** Agent is stationary and ready for new orders. */
    IDLE, 
    /** Agent is currently traveling towards a target destination. */
    MOVING, 
    /** Agent is performing a task (e.g., gathering, crafting). */
    WORKING, 
    /** Agent is recovering energy or HP. */
    RESTING
}
