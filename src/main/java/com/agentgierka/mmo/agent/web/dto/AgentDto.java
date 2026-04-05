package com.agentgierka.mmo.agent.web.dto;

import com.agentgierka.mmo.agent.model.AgentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Data Transfer Object for Agent information in the REST API.
 */
@Data
@Builder
public class AgentDto {
    private UUID id;
    private String name;
    private UUID currentLocationId; // Spójność z nazwą pola currentLocation w encji Agent
    private Integer x;
    private Integer y;
    private Integer targetX;
    private Integer targetY;
    private Integer speed;
    private AgentStatus status;
    private String currentActionDescription;

    // --- RPG Stats ---
    private Integer hp;
    private Integer maxHp;
    private Integer experience;
    private Integer expThreshold;
    private Integer level;
}
