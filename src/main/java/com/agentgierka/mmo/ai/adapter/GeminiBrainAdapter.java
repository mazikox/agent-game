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
                    You are the autonomous brain of an agent in a persistent MMO idle game.
                    The world consists of distinct 'Lands' (Locations). Each land is a grid with specific Width and Height.
                    Your goal is to fulfill your 'Current Goal'.

                    Coordinate System Rules:
                    - (0, 0) is the Top-Left corner (North-West).
                    - Y increases towards the South (Bottom). Y=0 is the top edge.
                    - X increases towards the East (Right). X=0 is the left edge.
                    - STAY WITHIN THE MAP: Your target coordinates must be between 0 and the map's Width/Height.

                    Navigation Rules:
                    1. If the 'Current Goal' is a movement command within the current land (e.g., "go to the top", "go to center"), calculate the appropriate (X, Y) and set 'status' to 'MOVING'.
                    2. PORTALS are 'World Gates'. Use them ONLY if your goal requires traveling to a DIFFERENT land.
                    3. To use a portal, set 'targetX' and 'targetY' to the portal's coordinates.
                    4. If you are already at the desired location or have no clear path, set 'status' to 'IDLE'.
                    5. Provide a brief, role-play style 'actionSummary' explaining your reasoning (e.g., "I'm heading North to reach the border").
                    
                    Always respond in JSON.
                    """ + "\n" + converter.getFormat();

            String userText = """
                    Current Perception:
                    Name: %s
                    Map Size: %dx%d
                    Current Coordinates: (%d, %d)
                    Current Land: %s - %s
                    Current Goal: %s
                    Last Action: %s
                    Visible Portals (World Gates): %s
                    
                    Decide your next logical step.
                    """.formatted(
                    perception.name(),
                    perception.mapWidth(),
                    perception.mapHeight(),
                    perception.x(),
                    perception.y(),
                    perception.locationName(),
                    perception.locationDescription(),
                    perception.currentGoal(),
                    perception.lastActionDescription(),
                    perception.nearbyObjects()
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

            Thought thought = converter.convert(content);
            return validateThought(thought, perception);

        } catch (Exception e) {
            return createFallbackThought("AI thinking failed: " + e.getMessage());
        }
    }

    private Thought validateThought(Thought thought, Perception perception) {
        if (thought == null) return createFallbackThought("Parsing failed.");

        // Hallucination Guard: Validate coordinates against map boundaries
        if (thought.targetX() != null && thought.targetY() != null) {
            boolean outOfBounds = thought.targetX() < 0 || thought.targetX() > perception.mapWidth() ||
                                 thought.targetY() < 0 || thought.targetY() > perception.mapHeight();
            
            if (outOfBounds) {
                return thought.toBuilder()
                        .status("IDLE")
                        .actionSummary("AI hallucinated coordinates (" + thought.targetX() + "," + thought.targetY() + ") outside map boundaries. Standing by.")
                        .targetX(null)
                        .targetY(null)
                        .build();
            }
        }
        return thought;
    }

    private Thought createFallbackThought(String reason) {
        return Thought.builder()
                .actionSummary("AI had trouble thinking (" + reason + "). Standing by.")
                .status("IDLE")
                .build();
    }
}
