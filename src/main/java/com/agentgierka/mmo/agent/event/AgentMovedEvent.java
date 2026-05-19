package com.agentgierka.mmo.agent.event;

import java.util.UUID;

/**
 * Event published when an agent moves to a new position.
 * Modeled after WoW's UNIT_MOVED event to transmit only spatial parameters.
 */
public record AgentMovedEvent(
    UUID agentId,
    String agentName,
    UUID locationId,
    int x,
    int y,
    long version
) {}
