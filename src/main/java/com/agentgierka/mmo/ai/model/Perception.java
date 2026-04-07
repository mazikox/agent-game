package com.agentgierka.mmo.ai.model;

import lombok.Builder;
import java.util.List;

@Builder
public record Perception(
    String name,
    Integer x,
    Integer y,
    Integer mapWidth,
    Integer mapHeight,
    String locationName,
    String locationDescription,
    String currentGoal,
    String lastActionDescription,
    List<String> nearbyObjects
) {}

