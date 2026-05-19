package com.agentgierka.mmo.agent.event;

import java.util.UUID;

/**
 * Event published when an agent acquires or loses a combat target.
 * Modeled after WoW's PLAYER_TARGET_CHANGED to transmit target updates only.
 */
public record AgentCombatTargetChangedEvent(
    UUID agentId,
    UUID targetId,       // null means target lost
    String targetName,
    int targetHp,
    int targetMaxHp,
    long version
) {}
