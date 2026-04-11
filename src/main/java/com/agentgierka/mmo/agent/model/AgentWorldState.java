package com.agentgierka.mmo.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import java.io.Serializable;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the volatile, real-time state of an agent stored in Redis.
 * This is used for high-frequency updates (e.g., movement ticks).
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentWorldState implements Serializable {
    private UUID agentId;
    private String agentName;
    private Integer x;
    private Integer y;
    private Integer targetX;
    private Integer targetY;
    private UUID currentLocationId;
    private AgentStatus status;
    
    @Builder.Default
    private Integer speed = 1;

    @Builder.Default
    private long version = 0;

    public void incrementVersion() {
        this.version++;
    }

    public static AgentWorldState fromAgent(Agent agent) {
        return AgentWorldState.builder()
                .agentId(agent.getId())
                .agentName(agent.getName())
                .x(agent.getX())
                .y(agent.getY())
                .targetX(agent.getTargetX())
                .targetY(agent.getTargetY())
                .currentLocationId(agent.getCurrentLocation() != null ? agent.getCurrentLocation().getId() : null)
                .status(agent.getStatus())
                .speed(agent.getSpeed())
                .version(0)
                .build();
    }

    @JsonIgnore
    public boolean updatePosition() {
        if (targetX == null || targetY == null || isAtTarget()) {
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
