package com.agentgierka.mmo.combat.event;

import java.util.UUID;

/**
 * Event published when a new combat log entry is generated.
 */
public record CombatLogEvent(UUID agentId, String message) {
}
