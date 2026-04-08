package com.agentgierka.mmo.agent.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for assigning a new goal to an agent.
 */
public record AssignGoalRequest(
    @NotBlank(message = "Goal cannot be empty")
    String goal
) {}
