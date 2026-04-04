package com.agentgierka.mmo.agent.event;

import java.util.UUID;

/**
 * Domain event published when a new goal is assigned to an agent.
 * Consumed by AI listeners that must run after the transaction commits.
 */
public record GoalAssignedEvent(UUID agentId) {}
