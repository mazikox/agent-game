package com.agentgierka.mmo.creature.web.dto;

import java.util.UUID;

public record SpawnPointDto(
        UUID id,
        UUID templateId,
        String templateName,
        UUID locationId,
        int centerX,
        int centerY,
        int wanderRadius,
        int respawnSeconds,
        boolean active
) {}
