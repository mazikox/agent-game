package com.agentgierka.mmo.agent.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum GoalExecutionMode {
    SIMPLE,
    BEHAVIOR_TREE
}
