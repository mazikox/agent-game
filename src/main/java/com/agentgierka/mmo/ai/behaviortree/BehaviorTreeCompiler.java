package com.agentgierka.mmo.ai.behaviortree;

import com.agentgierka.mmo.ai.adapter.GoalConditionFactory;
import com.agentgierka.mmo.ai.adapter.dto.BehaviorTreeDto;
import com.agentgierka.mmo.ai.behaviortree.composite.SelectorNode;
import com.agentgierka.mmo.ai.behaviortree.composite.SequenceNode;
import com.agentgierka.mmo.ai.behaviortree.decorator.RepeatUntilNode;
import com.agentgierka.mmo.ai.behaviortree.leaf.*;
import com.agentgierka.mmo.ai.model.Direction;
import com.agentgierka.mmo.ai.model.Perception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class BehaviorTreeCompiler {

    private final GoalConditionFactory goalConditionFactory;

    public BehaviorNode compile(BehaviorTreeDto dto, Perception perception) {
        return buildNode(dto, perception, 0);
    }

    private BehaviorNode buildNode(BehaviorTreeDto dto, Perception perception, int depth) {
        if (depth > 10) {
            throw new IllegalStateException("Behavior tree is too deep (exceeded maximum depth of 10)");
        }
        if (dto == null || dto.getType() == null) return null;

        return switch (dto.getType().toUpperCase()) {
            case "SEQUENCE" -> new SequenceNode(buildChildren(dto.getChildren(), perception, depth + 1));
            case "SELECTOR" -> new SelectorNode(buildChildren(dto.getChildren(), perception, depth + 1));
            case "REPEAT_UNTIL" -> {
                BehaviorTreeDto childDto = dto.getChild();
                if (childDto == null && dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                    childDto = dto.getChildren().get(0);
                }

                if (dto.getChildren() != null && dto.getChildren().size() > 1) {
                    throw new IllegalArgumentException("REPEAT_UNTIL node can only have exactly ONE child.");
                }

                yield new RepeatUntilNode(
                        goalConditionFactory.parse(dto.getCondition()),
                        buildNode(childDto, perception, depth + 1)
                );
            }
            case "FIND_NEAREST_CREATURE" -> new FindNearestCreatureAction();
            case "MOVE_TO_TARGET" -> new MoveToTargetAction();
            case "ATTACK_TARGET" -> new AttackTargetAction();
            case "FIND_PORTAL" -> new FindPortalAction(dto.getTargetLocation());
            case "ENTER_PORTAL" -> new EnterPortalAction();
            case "MOVE_DIRECTION" -> compileMoveDirection(dto, perception);
            case "MOVE_TO_POSITION" -> new MoveToPositionAction(
                    dto.getRawX() != null ? dto.getRawX() : 0,
                    dto.getRawY() != null ? dto.getRawY() : 0
            );
            case "IDLE" -> new IdleAction();
            default -> {
                log.error("Unknown Behavior Tree node type received from AI: '{}'. Failing compilation.", dto.getType());
                throw new IllegalArgumentException("Unknown node type: " + dto.getType());
            }
        };
    }

    private BehaviorNode compileMoveDirection(BehaviorTreeDto dto, Perception perception) {
        Direction dir = null;
        try {
            if (dto.getDirection() != null) {
                dir = Direction.valueOf(dto.getDirection().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            log.warn("AI provided invalid direction: '{}'. Defaulting to IDLE.", dto.getDirection());
        }
        int steps = dto.getSteps() != null ? dto.getSteps() : 0;
        
        if (dir == null || steps <= 0) {
            log.error("AI provided invalid MOVE_DIRECTION parameters: dir={}, steps={}", dir, steps);
            throw new IllegalArgumentException("Invalid MOVE_DIRECTION parameters");
        }

        int agentX = perception.x() != null ? perception.x() : 0;
        int agentY = perception.y() != null ? perception.y() : 0;
        int maxW = perception.mapWidth() != null ? perception.mapWidth() : Integer.MAX_VALUE;
        int maxH = perception.mapHeight() != null ? perception.mapHeight() : Integer.MAX_VALUE;

        int targetX = agentX;
        int targetY = agentY;

        switch (dir) {
            case UP -> targetY = Math.max(0, agentY - steps);
            case DOWN -> targetY = Math.min(maxH, agentY + steps);
            case LEFT -> targetX = Math.max(0, agentX - steps);
            case RIGHT -> targetX = Math.min(maxW, agentX + steps);
        }

        log.info("Compiling MOVE_DIRECTION {} {} steps into MoveToPosition({}, {}) from starting point ({}, {})", 
                 dir, steps, targetX, targetY, agentX, agentY);
                 
        return new MoveToPositionAction(targetX, targetY);
    }

    private List<BehaviorNode> buildChildren(List<BehaviorTreeDto> dtos, Perception perception, int depth) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(dto -> buildNode(dto, perception, depth))
                .collect(Collectors.toList());
    }
}
