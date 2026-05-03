package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.world.Location;
import java.util.UUID;

/**
 * Event published when an agent reaches its destination coordinates or a waypoint.
 * This is the unified event for all movement arrivals (walking, portal, teleport).
 */
public record AgentArrivedAtWaypointEvent(
    UUID agentId,
    String agentName,
    Location location,
    Integer x,
    Integer y,
    MovementType type
) {}
