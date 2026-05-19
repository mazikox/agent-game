package com.agentgierka.mmo.agent.web;

import java.util.UUID;

/**
 * Data Transfer Object for broadcasting spatial coordinate updates via WebSocket.
 */
public record AgentPositionDto(
    int x,
    int y,
    UUID locationId,
    long version
) {}
