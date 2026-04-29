package com.agentgierka.mmo.ai.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record Decision(
    String actionType,
    Integer targetIndex,
    String qualifier,
    Integer rawX,
    Integer rawY,
    String direction,
    Integer steps,
    String actionSummary
) {}
