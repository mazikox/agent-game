package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.agent.model.AgentStatus;
import java.util.UUID;

/**
 * Event published when an agent transitions between statuses (e.g., IDLE, WALKING, IN_COMBAT).
 * Modeled after WoW's UNIT_FLAGS updates to transmit status changes only.
 */
public record AgentStatusChangedEvent(
    UUID agentId,
    AgentStatus status,
    String actionDescription,
    long version
) {}
