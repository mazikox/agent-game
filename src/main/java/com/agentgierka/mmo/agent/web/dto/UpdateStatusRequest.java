package com.agentgierka.mmo.agent.web.dto;

import com.agentgierka.mmo.agent.model.AgentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating an agent's status.
 */
public record UpdateStatusRequest(
    @NotNull(message = "Status is required")
    AgentStatus status,

    @NotBlank(message = "Description is required")
    String description
) {}
