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
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.port.GoalPlanner;
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
    private final ObjectMapper objectMapper;

    public GeminiGoalPlannerAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<BehaviorNode> planGoal(String goal, Perception perception) {
        try {
            String systemText = """
                    You are a high-level Strategy Planner for an MMO AI agent.
                    Your job is to decide whether a player's goal requires a complex Behavior Tree OR a simple atomic action.

                    CRITICAL RULE:
                    - If the goal is a SINGLE ATOMIC action (e.g., "go left", "move down 5", "wait", "stand still"), return {"isSimple": true}. Do NOT generate a Behavior Tree for simple tasks!
                    - If the goal requires repetitive tasks, multiple sequential steps, or conditions (e.g., "grind until level 5", "kill a monster then go to the city"), return {"isSimple": false, "tree": { ... }}.

                    Behavior Tree Node Types available:
                    1. SEQUENCE: Executes children in order.
                    2. SELECTOR: Executes children until first success.
                    3. REPEAT_UNTIL: Repeats its single child until a condition is met.
                    4. FIND_NEAREST_CREATURE: Finds closest alive monster (Leaf).
                    5. MOVE_TO_TARGET: Moves towards the targeted creature/portal (Leaf).
                    6. ATTACK_TARGET: Attacks the creature (Leaf). Wait mode for manual combat.
                    7. FIND_PORTAL: Finds portal by name. Requires "targetLocation" (Leaf).
                    8. ENTER_PORTAL: Uses the portal (Leaf).

                    Conditions available for REPEAT_UNTIL:
                    - "level >= X": X is the target level.

                    Output MUST be JSON conforming to this structure:
                    {
                      "isSimple": boolean,
                      "tree": {
                        "type": "SEQUENCE" | "SELECTOR" | "REPEAT_UNTIL" | "FIND_NEAREST_CREATURE" | "MOVE_TO_TARGET" | "ATTACK_TARGET" | "FIND_PORTAL" | "ENTER_PORTAL",
                        "targetLocation": string (only for FIND_PORTAL),
                        "condition": string (only for REPEAT_UNTIL),
                        "children": [ recursive tree nodes ]
                      }
                    }

                    Return raw JSON only.
                    """;

            String userPrompt = "Current Goal: " + goal + "\nAgent Level: " + (perception != null ? 1 : 1); // Simple placeholder for level check

            String response = chatModel.call(new Prompt(systemText + "\n" + userPrompt)).getResult().getOutput().getText();
            
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");
            if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
                response = response.substring(jsonStart, jsonEnd + 1).trim();
            }

            log.info("RAW Planner AI Response for agent: \n{}", response);
            PlannerResponse planned = objectMapper.readValue(response, PlannerResponse.class);

            if (planned.isSimple() || planned.getTree() == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(buildNode(planned.getTree()));

        } catch (Exception e) {
            log.error("CRITICAL ERROR while planning Behavior Tree via Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to plan behavior tree: " + e.getMessage(), e);
        }
    }

    private BehaviorNode buildNode(BehaviorTreeDto dto) {
        if (dto == null || dto.getType() == null) return null;

        return switch (dto.getType().toUpperCase()) {
            case "SEQUENCE" -> new SequenceNode(buildChildren(dto.getChildren()));
            case "SELECTOR" -> new SelectorNode(buildChildren(dto.getChildren()));
            case "REPEAT_UNTIL" -> new RepeatUntilNode(
                    ctx -> {
                        if (dto.getCondition() != null && dto.getCondition().startsWith("level >= ")) {
                            try {
                                int targetLvl = Integer.parseInt(dto.getCondition().replace("level >= ", "").trim());
                                return ctx.agent().getStats().getLevel() >= targetLvl;
                            } catch (NumberFormatException e) {
                                return true; // fallback to stop loop
                            }
                        }
                        return false;
                    },
                    buildNode(dto.getChild())
            );
            case "FIND_NEAREST_CREATURE" -> new FindNearestCreatureAction();
            case "MOVE_TO_TARGET" -> new MoveToTargetAction();
            case "ATTACK_TARGET" -> new AttackTargetAction();
            case "FIND_PORTAL" -> new FindPortalAction(dto.getTargetLocation());
            case "ENTER_PORTAL" -> new EnterPortalAction();
            default -> throw new IllegalArgumentException("Unknown BT node type: " + dto.getType());
        };
    }

    private List<BehaviorNode> buildChildren(List<BehaviorTreeDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(this::buildNode)
                .collect(Collectors.toList());
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class PlannerResponse {
    @JsonProperty("isSimple")
    private boolean isSimple;
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
    private List<BehaviorTreeDto> children;

    public BehaviorTreeDto getChild() {
        return children != null && !children.isEmpty() ? children.get(0) : null;
    }
}
