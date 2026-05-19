package com.agentgierka.mmo.agent.web;

import java.util.UUID;

/**
 * Data Transfer Object for broadcasting combat target changes via WebSocket.
 */
public record AgentTargetDto(
    UUID targetId,       // null means target lost
    String targetName,
    int targetHp,
    int targetMaxHp,
    long version
) {}
