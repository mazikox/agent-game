package com.agentgierka.mmo.ai.event;

import java.util.UUID;

/**
 * Event published when an agent is in an IDLE state but has an active goal,
 * indicating that the AI should perform another thinking cycle (tick).
 * Used to break synchronous execution loops and prevent stack overflows.
 */
public record AiThinkingRequiredEvent(UUID agentId) {
}
