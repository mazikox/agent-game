package com.agentgierka.mmo.ai.adapter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorTreeDto {
    @JsonPropertyDescription("The type of behavior tree node (e.g., SEQUENCE, MOVE_DIRECTION, IDLE)")
    private String type;

    @JsonPropertyDescription("The destination location name for portal finding")
    private String targetLocation;

    @JsonPropertyDescription("The condition string for REPEAT_UNTIL nodes")
    private String condition;

    @JsonPropertyDescription("The movement direction: UP, DOWN, LEFT, or RIGHT")
    private String direction;

    @JsonPropertyDescription("The number of steps to move")
    private Integer steps;

    @JsonPropertyDescription("Absolute X coordinate")
    private Integer rawX;

    @JsonPropertyDescription("Absolute Y coordinate")
    private Integer rawY;

    @JsonPropertyDescription("List of child nodes for composite nodes")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<BehaviorTreeDto> children;

    public BehaviorTreeDto getChild() {
        return children != null && !children.isEmpty() ? children.get(0) : null;
    }
}
