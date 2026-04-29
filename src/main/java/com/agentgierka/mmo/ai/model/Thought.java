package com.agentgierka.mmo.ai.model;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record Thought(
    List<Decision> actions
) {}
