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
        String systemText = """
                You are the autonomous brain of an agent in a persistent MMO idle game.
                Your goal is to fulfill your 'Current Goal' by navigating the world.
                You receive the 'Current Perception', including your coordinates and nearby portals.

                Rules:
                1. If the 'Current Goal' requires changing maps, look for a portal in 'Visible Objects/Portals'.
                2. To use a portal, set 'targetX' and 'targetY' to the portal's coordinates and set 'status' to 'MOVING'.
                3. Once you reach the portal, the system will teleport you.
                4. If you have multiple steps in your goal, focus on the current one in 'actionSummary' and update 'nextGoal' for the future.
                5. Always respond in JSON. Focus on logical, autonomous decision-making.
                """ + "\n" + converter.getFormat();

        String userText = """
                Current Perception:
                Name: %s
                Coordinates: (%d, %d)
                Location: %s - %s
                Current Goal: %s
                Last Action: %s
                Visible Objects/Portals: %s
                
                Decide what to do next.
                """.formatted(
                perception.name(),
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

        String content = chatModel.call(prompt).getResult().getOutput().getText();
        return converter.convert(content);
    }
}
