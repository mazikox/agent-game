package com.agentgierka.mmo.ai.model;

import lombok.Builder;

@Builder
public record Thought(
    String actionSummary,
    String nextGoal,
    Integer targetX,
    Integer targetY,
    String status
) {}
