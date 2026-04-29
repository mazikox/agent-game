package com.agentgierka.mmo.ai.adapter;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
public class GeminiBrainAdapter implements Brain {

    private final ChatModel chatModel;
    private final BeanOutputConverter<Thought> converter;

    public GeminiBrainAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(Thought.class);
    }

    @Override
    public Thought think(Perception perception) {
        try {
            String systemText = """
                    You are a LITERAL command interpreter for an agent in an MMO game.
                    Your ONLY job is to translate the 'Current Goal' into the MINIMUM number of game actions needed.
                    
                    CRITICAL RULES:
                    1. Interpret the goal LITERALLY. If the user says 'go 5 steps left', return exactly ONE MOVE_DIRECTION action with direction=LEFT, steps=5. Do NOT invent extra steps.
                    2. Most goals require exactly 1 action. Use multiple actions ONLY when the goal EXPLICITLY asks for distinct sequential steps (e.g., 'go right 3 then go down 2').
                    3. NEVER return more actions than the user asked for. Maximum is 3 actions.
                    4. If the goal means stop, wait, stand still, idle, or do nothing → return a SINGLE action with actionType=IDLE.
                    
                    Map Coordinate System:
                    - (0, 0) is the top-left corner of the map.
                    - Going DOWN INCREASES Y. Going UP DECREASES Y.
                    - Going RIGHT INCREASES X. Going LEFT DECREASES X.

                    Available Action Types:
                    - MOVE_DIRECTION: Move N steps in a cardinal direction. Set 'direction' (UP/DOWN/LEFT/RIGHT) and 'steps' (integer). Use this for directional commands like 'go left', 'move up 10', etc.
                    - MOVE_TO_CREATURE: Go to a visible monster.
                    - MOVE_TO_PORTAL: Go to a visible teleport gate.
                    - MOVE_TO_POSITION: Go to absolute coordinates (rawX, rawY). Use ONLY when explicit coordinates are given.
                    - IDLE: Do nothing. Stay in place. Use for: stop, wait, stand, rest, halt, etc.

                    Targeting Rules (for MOVE_TO_CREATURE / MOVE_TO_PORTAL):
                    - Use 'targetIndex' (0-indexed) to pick from the Visible lists.
                    - Or use 'qualifier': NEAREST, NEAREST_OTHER, or SPECIFIC.

                    Field Rules:
                    - 'rawX'/'rawY': fill ONLY for MOVE_TO_POSITION.
                    - 'direction'/'steps': fill ONLY for MOVE_DIRECTION.
                    - 'actionSummary': short description in the SAME LANGUAGE as the goal.

                    Always respond in JSON.
                    """ + "\n" + converter.getFormat();

            String userText = """
                    Current Perception:
                    Name: %s
                    Map Size: %dx%d
                    Current Coordinates: (%d, %d)
                    Current Land: %s - %s
                    Current Goal: %s
                    Memory Log (Recent Events):
                    %s
                    Last Action: %s
                    Visible Portals (World Gates): %s
                    Visible Creatures (Monsters): %s
                    
                    Translate this goal into the appropriate action(s).
                    """.formatted(
                    perception.name(),
                    perception.mapWidth(),
                    perception.mapHeight(),
                    perception.x(),
                    perception.y(),
                    perception.locationName(),
                    perception.locationDescription(),
                    perception.currentGoal(),
                    perception.memoryLog() != null && !perception.memoryLog().isEmpty() ? 
                            String.join("\n", perception.memoryLog()) : "No previous actions recorded.",
                    perception.lastActionDescription(),
                    perception.nearbyObjects(),
                    perception.visibleCreatures()
            );

            Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemText),
                new UserMessage(userText)
            ));

            var response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return createFallbackThought("AI provided no response.");
            }

            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                return createFallbackThought("AI response was empty.");
            }

            log.info("RAW AI Response for agent {}: \n{}", perception.name(), content);

            return converter.convert(content);

        } catch (Exception e) {
            return createFallbackThought("AI thinking failed: " + e.getMessage());
        }
    }

    private Thought createFallbackThought(String reason) {
        return Thought.builder()
                .actions(List.of(com.agentgierka.mmo.ai.model.Decision.builder()
                        .actionType("IDLE")
                        .actionSummary("AI had trouble thinking (" + reason + "). Standing by.")
                        .build()))
                .build();
    }
}
