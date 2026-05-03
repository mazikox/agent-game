package com.agentgierka.mmo.ai.adapter;

import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeCompiler;
import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.port.GoalPlanner;
import com.agentgierka.mmo.ai.adapter.dto.BehaviorTreeDto;
import com.agentgierka.mmo.ai.adapter.dto.PlannerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Profile("!test")
public class GeminiGoalPlannerAdapter implements GoalPlanner {

    private final ChatModel chatModel;
    private final BehaviorTreeCompiler compiler;
    private final BeanOutputConverter<PlannerResponse> converter;

    public GeminiGoalPlannerAdapter(ChatModel chatModel, BehaviorTreeCompiler compiler) {
        this.chatModel = chatModel;
        this.compiler = compiler;
        this.converter = new BeanOutputConverter<>(PlannerResponse.class);
    }

    @Override
    public Optional<BehaviorNode> planGoal(String goal, Perception perception) {
        try {
            String systemText = """
                    ROLE: MMO AI Logic Engine.
                    TASK: Translate the player's CURRENT GOAL into a valid Behavior Tree JSON.

                    API RESPONSE SCHEMA (JSON):
                    {
                      "tree": {
                        "type": "STRING",           // SEQUENCE, SELECTOR, REPEAT_UNTIL, MOVE_DIRECTION, etc.
                        "children": "ARRAY",        // List of node objects. IMPORTANT: Do NOT wrap child nodes in another "tree" field!
                        "condition": "STRING",      // For REPEAT_UNTIL ONLY.
                        "direction": "STRING",      // For MOVE_DIRECTION.
                        "steps": "NUMBER",          // For MOVE_DIRECTION.
                        "rawX": "NUMBER",           // For MOVE_TO_POSITION.
                        "rawY": "NUMBER",           // For MOVE_TO_POSITION.
                        "targetLocation": "STRING"  // For FIND_PORTAL.
                      }
                    }

                    BEHAVIOR RULES:
                    1. COMBAT: To attack, use SEQUENCE: [FIND_NEAREST_CREATURE, MOVE_TO_TARGET, ATTACK_TARGET].
                    2. NAVIGATION: Use visible "Portals/Objects" from perception to travel. Use FIND_PORTAL with 'targetLocation' field.
                    3. STRICT TYPES: Use ONLY node types from FIELD DEFINITIONS. Do NOT hallucinate types (e.g., do NOT use FIND_NEAREST_PORTAL).
                    4. REPEAT_UNTIL: Child node is executed until 'condition' is TRUE.
                    5. NESTING: Children MUST be raw objects. Only the ROOT object has the "tree" key.
                    6. Output ONLY raw JSON. No markdown.
                    """;

            int agentLevel = perception.level() != null ? perception.level() : 1;

            String userPrompt = String.format("""
                    Recent History (newest first):
                    %s

                    CURRENT GOAL: %s

                    Agent State:
                    - Name: %s (Level %d)
                    - Position: (%d, %d)
                    - Location: %s (%dx%d)

                    Visible World:
                    - Portals/Objects: %s
                    - Creatures: %s
                    """,
                    perception.memoryLog().isEmpty() ? "No history." : String.join("\n", perception.memoryLog()),
                    goal,
                    perception.name(), agentLevel,
                    perception.x(), perception.y(),
                    perception.locationName(), perception.mapWidth(), perception.mapHeight(),
                    perception.nearbyObjects().isEmpty() ? "None" : String.join("\n", perception.nearbyObjects()),
                    perception.visibleCreatures().isEmpty() ? "None"
                            : String.join("\n", perception.visibleCreatures()));

            String response = chatModel.call(new Prompt(systemText + "\n" + userPrompt)).getResult().getOutput()
                    .getText();
            log.info("RAW Planner AI Response for agent {}: \n{}", perception.name(), response);

            // Safer JSON extraction: find first { and last }
            int firstBrace = response.indexOf("{");
            int lastBrace = response.lastIndexOf("}");
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                response = response.substring(firstBrace, lastBrace + 1);
            }
            response = response.trim();

            PlannerResponse planned = converter.convert(response);

            if (planned == null || planned.getTree() == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(compiler.compile(planned.getTree(), perception));

        } catch (Exception e) {
            log.error("CRITICAL ERROR while planning Behavior Tree via Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to plan behavior tree: " + e.getMessage(), e);
        }
    }
}
