package com.agentgierka.mmo.agent.event;

import java.util.UUID;

public record AgentConsoleLogEvent(UUID agentId, String message) {}
