package com.agentgierka.mmo.agent.web;

/**
 * Data Transfer Object for broadcasting agent health and healing changes via WebSocket.
 */
public record AgentHealthDto(
    int hp,
    int maxHp,
    long version
) {}
