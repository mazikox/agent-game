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

@Component
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
                    You are the intent classification brain of an agent in a persistent MMO idle game.
                    Your job is to break down the 'Current Goal' into a sequence of logical steps (List of Decisions).
                    
                    Map Coordinate System:
                    - (0, 0) is the top-left corner of the map.
                    - Going DOWN INCREASES Y. Going UP DECREASES Y.
                    - Going RIGHT INCREASES X. Going LEFT DECREASES X.

                    Available Action Types:
                    - MOVE_TO_CREATURE: Go to a monster.
                    - MOVE_TO_PORTAL: Go to a teleport gate.
                    - MOVE_TO_POSITION: Go to absolute coordinates. Use this WHENEVER explicit coordinates (like '50,30') are provided.
                    - MOVE_RELATIVE: Step by a relative amount (e.g., offset from current position).
                    - IDLE: Stand still.
                    - UNKNOWN: Use when completely unsure.

                    Targeting Rules:
                    1. Use 'targetIndex' (0-indexed integer) to pick a specific monster or portal from the lists provided in your Perception.
                    2. If you don't use targetIndex, use 'qualifier':
                       - NEAREST: Pick the closest one.
                       - NEAREST_OTHER: Pick the closest one that isn't the current one.
                       - SPECIFIC: Target a specific named entity.
                    3. 'rawX' and 'rawY' MUST be filled for MOVE_TO_POSITION (use absolute X,Y) or MOVE_RELATIVE (use relative offset like X=5, Y=-3).

                    Multi-Step Sequences:
                    - If the goal says "do X and then do Y", return exactly TWO actions in the actions array.

                    Language Rule:
                    - Write your 'actionSummary' in the same language as the 'Current Goal' (e.g., Polish if user typed Polish).

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
                    
                    Decide your next logical steps.
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
