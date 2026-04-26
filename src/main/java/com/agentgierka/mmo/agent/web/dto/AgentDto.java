package com.agentgierka.mmo.agent.web.dto;

import com.agentgierka.mmo.agent.model.AgentStatus;

import java.util.UUID;

/**
 * Data Transfer Object for Agent information in the REST API.
 */
public record AgentDto(
        UUID id,
        String name,
        UUID currentLocationId,
        Integer x,
        Integer y,
        Integer targetX,
        Integer targetY,
        Integer speed,
        AgentStatus status,
        String currentActionDescription,
        String currentTask,
        String goal,
        Integer mapWidth,
        Integer mapHeight,

        // --- RPG Stats ---
        Integer hp,
        Integer maxHp,
        Integer experience,
        Integer expThreshold,
        Integer level,
        UUID targetId) {
}
