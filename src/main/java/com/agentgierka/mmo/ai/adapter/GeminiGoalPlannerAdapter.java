package com.agentgierka.mmo.ai.adapter;

import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.composite.SelectorNode;
import com.agentgierka.mmo.ai.behaviortree.composite.SequenceNode;
import com.agentgierka.mmo.ai.behaviortree.decorator.RepeatUntilNode;
import com.agentgierka.mmo.ai.behaviortree.leaf.AttackTargetAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.ConditionNode;
import com.agentgierka.mmo.ai.behaviortree.leaf.EnterPortalAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.FindNearestCreatureAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.FindPortalAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.MoveToTargetAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.MoveDirectionAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.MoveToPositionAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.IdleAction;
import com.agentgierka.mmo.ai.model.Direction;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.port.GoalPlanner;
import com.agentgierka.mmo.combat.service.CombatService;
import com.agentgierka.mmo.world.PortalRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@Profile("!test")
public class GeminiGoalPlannerAdapter implements GoalPlanner {

    private final ChatModel chatModel;
    private final PortalRepository portalRepository;
    private final CombatService combatService;
    private final GoalConditionFactory goalConditionFactory;
    private final ObjectMapper objectMapper;

    public GeminiGoalPlannerAdapter(ChatModel chatModel, PortalRepository portalRepository, CombatService combatService, GoalConditionFactory goalConditionFactory) {
        this.chatModel = chatModel;
        this.portalRepository = portalRepository;
        this.combatService = combatService;
        this.goalConditionFactory = goalConditionFactory;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<BehaviorNode> planGoal(String goal, Perception perception) {
        try {
            String systemText = """
                    You are a high-level Strategy Planner for an MMO AI agent.
                    Your job is to generate a Behavior Tree for a player's goal.
                    
                    CRITICAL RULE:
                    - You MUST ALWAYS return a Behavior Tree. Even if the goal is a SINGLE ATOMIC action (e.g., "go left", "move down 5", "wait", "stand still"), return a single-node tree.
                    - ALWAYS use MOVE_TO_TARGET between FIND_NEAREST_CREATURE and ATTACK_TARGET. An agent cannot attack from a distance!

                    Behavior Tree Node Types available:
                    1. SEQUENCE: Executes children in order.
                    2. SELECTOR: Executes children until first success.
                    3. REPEAT_UNTIL: Repeats exactly ONE child node (must be a SEQUENCE or leaf) until condition is met. 
                       Always wrap multiple steps inside a SEQUENCE and pass that as the single child.
                       Example: REPEAT_UNTIL -> SEQUENCE -> [FIND_NEAREST_CREATURE, MOVE_TO_TARGET, ATTACK_TARGET]
                    4. FIND_NEAREST_CREATURE: Finds closest alive monster (Leaf).
                    5. MOVE_TO_TARGET: Moves towards the targeted creature/portal (Leaf).
                    6. ATTACK_TARGET: Attacks the creature (Leaf). Wait mode for manual combat.
                    7. FIND_PORTAL: Finds portal by name. Requires "targetLocation" (Leaf).
                    8. ENTER_PORTAL: Uses the portal (Leaf).
                    9. MOVE_DIRECTION: Moves N steps in a given direction. Fields: direction (UP/DOWN/LEFT/RIGHT), steps (Leaf).
                    10. MOVE_TO_POSITION: Moves to absolute coordinates. Fields: rawX, rawY (Leaf).
                    11. IDLE: Stand still / do nothing. No extra fields (Leaf).

                    Conditions available for REPEAT_UNTIL:
                    - "killCount >= X": Repeat until X creatures are killed.
                    - "level >= X": Repeat until agent reaches level X.
                    - "expGained >= X": Repeat until X experience points are gained.
                    - "hpPercent <= X": Stop when HP drops below X percent.
                    - "locationReached == 'Name'": Stop when reaching location with name 'Name'.
                    - "AND(cond1, cond2)": Both conditions must be true.
                    - "OR(cond1, cond2)": At least one condition must be true.

                    Output MUST be JSON conforming to this structure:
                    {
                      "tree": {
                        "type": "SEQUENCE" | "SELECTOR" | "REPEAT_UNTIL" | "FIND_NEAREST_CREATURE" | "MOVE_TO_TARGET" | "ATTACK_TARGET" | "FIND_PORTAL" | "ENTER_PORTAL" | "MOVE_DIRECTION" | "MOVE_TO_POSITION" | "IDLE",
                        "targetLocation": string (only for FIND_PORTAL),
                        "condition": string (only for REPEAT_UNTIL),
                        "direction": string (only for MOVE_DIRECTION),
                        "steps": number (only for MOVE_DIRECTION),
                        "rawX": number (only for MOVE_TO_POSITION),
                        "rawY": number (only for MOVE_TO_POSITION),
                        "children": [ recursive tree nodes ] (MUST ALWAYS be an array, even for single child)
                      }
                    }

                    Return raw JSON only.
                    """;

            int agentLevel = perception.level() != null ? perception.level() : 1;

            String userPrompt = "Current Goal: " + goal + "\nAgent Level: " + agentLevel;

            String response = chatModel.call(new Prompt(systemText + "\n" + userPrompt)).getResult().getOutput().getText();
            
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");
            if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
                response = response.substring(jsonStart, jsonEnd + 1).trim();
            }

            log.info("RAW Planner AI Response for agent: \n{}", response);
            PlannerResponse planned = objectMapper.readValue(response, PlannerResponse.class);

            if (planned.getTree() == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(buildNode(planned.getTree(), 0));

        } catch (Exception e) {
            log.error("CRITICAL ERROR while planning Behavior Tree via Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to plan behavior tree: " + e.getMessage(), e);
        }
    }

    private BehaviorNode buildNode(BehaviorTreeDto dto, int depth) {
        if (depth > 10) {
            throw new IllegalStateException("Behavior tree is too deep (exceeded maximum depth of 10)");
        }
        if (dto == null || dto.getType() == null) return null;

        return switch (dto.getType().toUpperCase()) {
            case "SEQUENCE" -> new SequenceNode(buildChildren(dto.getChildren(), depth + 1));
            case "SELECTOR" -> new SelectorNode(buildChildren(dto.getChildren(), depth + 1));
            case "REPEAT_UNTIL" -> {
                if (dto.getChildren() != null && dto.getChildren().size() > 1) {
                    throw new IllegalArgumentException("REPEAT_UNTIL node can only have exactly ONE child. Gemini returned multiple.");
                }
                yield new RepeatUntilNode(
                        goalConditionFactory.parse(dto.getCondition()),
                        buildNode(dto.getChild(), depth + 1)
                );
            }
            case "FIND_NEAREST_CREATURE" -> new FindNearestCreatureAction();
            case "MOVE_TO_TARGET" -> new MoveToTargetAction();
            case "ATTACK_TARGET" -> new AttackTargetAction();
            case "FIND_PORTAL" -> new FindPortalAction(dto.getTargetLocation());
            case "ENTER_PORTAL" -> new EnterPortalAction();
            case "MOVE_DIRECTION" -> new MoveDirectionAction(
                    dto.getDirection() != null ? Direction.valueOf(dto.getDirection().toUpperCase()) : null,
                    dto.getSteps() != null ? dto.getSteps() : 0
            );
            case "MOVE_TO_POSITION" -> new MoveToPositionAction(
                    dto.getRawX() != null ? dto.getRawX() : 0,
                    dto.getRawY() != null ? dto.getRawY() : 0
            );
            case "IDLE" -> new IdleAction();
            default -> throw new IllegalArgumentException("Unknown BT node type: " + dto.getType());
        };
    }

    private List<BehaviorNode> buildChildren(List<BehaviorTreeDto> dtos, int depth) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(dto -> buildNode(dto, depth))
                .collect(Collectors.toList());
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class PlannerResponse {
    private BehaviorTreeDto tree;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class BehaviorTreeDto {
    private String type;
    private String targetLocation;
    private String condition;
    private String direction;
    private Integer steps;
    private Integer rawX;
    private Integer rawY;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<BehaviorTreeDto> children;

    public BehaviorTreeDto getChild() {
        return children != null && !children.isEmpty() ? children.get(0) : null;
    }
}
