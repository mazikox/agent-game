package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.world.Location;
import java.util.UUID;

/**
 * Event published when an agent successfully reaches its final destination 
 * and no world interactions (like portals) were triggered.
 */
public record AgentGoalCompletedEvent(
    UUID agentId,
    String agentName,
    Location location,
    Integer x,
    Integer y
) {}
