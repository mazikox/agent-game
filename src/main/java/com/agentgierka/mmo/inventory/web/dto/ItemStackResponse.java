package com.agentgierka.mmo.inventory.web.dto;

import java.util.UUID;

public record ItemStackResponse(
    UUID id,
    String definitionId,
    String name,
    int width,
    int height,
    int gridIndex,
    int quantity,
    boolean stackable
) {}
