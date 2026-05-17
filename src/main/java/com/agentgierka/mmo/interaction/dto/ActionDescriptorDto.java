package com.agentgierka.mmo.interaction.dto;

public record ActionDescriptorDto(
    String actionId,
    String icon,
    String endpoint,
    String httpMethod,
    boolean enabled,
    ActionDisabledReasonDto disabledReason
) {}
