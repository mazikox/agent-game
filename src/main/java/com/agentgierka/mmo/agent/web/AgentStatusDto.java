package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.model.AgentStatus;

/**
 * Data Transfer Object for broadcasting behavior and status transitions via WebSocket.
 */
public record AgentStatusDto(
    AgentStatus status,
    String actionDescription,
    long version
) {}
