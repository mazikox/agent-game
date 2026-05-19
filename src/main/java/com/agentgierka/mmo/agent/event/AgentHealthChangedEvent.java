package com.agentgierka.mmo.agent.event;

import java.util.UUID;

/**
 * Event published when an agent's HP or MaxHP changes (e.g., damage, heal, level-up).
 * Modeled after WoW's UNIT_HEALTH event to transmit health updates only.
 */
public record AgentHealthChangedEvent(
    UUID agentId,
    int hp,
    int maxHp,
    long version
) {}
