package com.agentgierka.mmo.agent.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for agent movement request coordinates.
 */
public record MoveRequest(
    @NotNull(message = "X coordinate is required")
    @Min(value = 0, message = "X coordinate must be at least 0")
    Integer x,

    @NotNull(message = "Y coordinate is required")
    @Min(value = 0, message = "Y coordinate must be at least 0")
    Integer y
) {}
