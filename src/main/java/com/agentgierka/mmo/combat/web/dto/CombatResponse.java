package com.agentgierka.mmo.combat.web.dto;

import com.agentgierka.mmo.combat.model.CombatInstance;
import com.agentgierka.mmo.combat.model.CombatStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CombatResponse(
    UUID combatId,
    UUID agentId,
    UUID creatureInstanceId,
    CombatStatus status,
    int agentAp,
    int creatureAp,
    LocalDateTime startedAt
) {
}
