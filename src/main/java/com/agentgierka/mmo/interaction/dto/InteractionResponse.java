package com.agentgierka.mmo.interaction.dto;

import java.util.List;
import java.util.UUID;

public record InteractionResponse(
    UUID targetId,
    String targetType,
    String targetName,
    List<ActionDescriptorDto> actions
) {}
