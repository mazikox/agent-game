package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.world.Location;
import java.util.UUID;

/**
 * Event published when an agent reaches its destination coordinates.
 * This is a Spring ApplicationEvent.
 */
public record AgentArrivedEvent(
    UUID agentId,
    Location location,
    Integer x,
    Integer y,
    MovementType type
) {}
