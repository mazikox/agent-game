package com.agentgierka.mmo.creature.web.dto;

import java.util.UUID;

public record CreateSpawnPointRequest(
        UUID templateId,
        UUID locationId,
        int centerX,
        int centerY,
        int wanderRadius,
        int respawnSeconds
) {}
