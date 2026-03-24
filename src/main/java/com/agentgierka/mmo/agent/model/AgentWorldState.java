package com.agentgierka.mmo.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

/**
 * Represents the volatile, real-time state of an agent stored in Redis.
 * This is used for high-frequency updates (e.g., movement ticks).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorldState implements Serializable {
    private UUID agentId;
    private Integer x;
    private Integer y;
    private Integer targetX;
    private Integer targetY;
    private AgentStatus status;
}
