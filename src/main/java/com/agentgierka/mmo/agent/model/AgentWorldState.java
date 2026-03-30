package com.agentgierka.mmo.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import java.io.Serializable;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the volatile, real-time state of an agent stored in Redis.
 * This is used for high-frequency updates (e.g., movement ticks).
 */
@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentWorldState implements Serializable {
    private UUID agentId;
    private Integer x;
    private Integer y;
    private Integer targetX;
    private Integer targetY;
    private AgentStatus status;
    
    @Builder.Default
    private Integer speed = 1;

    @JsonIgnore
    public boolean updatePosition() {
        if (isAtTarget()) {
            return true;
        }

        int currentSpeed = this.speed != null ? this.speed : 1;

        if (!x.equals(targetX)) {
            int diff = targetX - x;
            int step = Math.min(Math.abs(diff), currentSpeed);
            x += (diff > 0 ? step : -step);
        }

        if (!y.equals(targetY)) {
            int diff = targetY - y;
            int step = Math.min(Math.abs(diff), currentSpeed);
            y += (diff > 0 ? step : -step);
        }

        return isAtTarget();
    }

    @JsonIgnore
    public boolean isAtTarget() {
        return x != null && y != null && x.equals(targetX) && y.equals(targetY);
    }
}
