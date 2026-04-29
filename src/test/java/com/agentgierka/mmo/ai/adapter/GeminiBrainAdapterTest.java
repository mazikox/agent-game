package com.agentgierka.mmo.ai.adapter;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.model.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiBrainAdapterTest {

    @Mock
    private ChatModel chatModel;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatResponse chatResponse;

    private GeminiBrainAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GeminiBrainAdapter(chatModel);
    }

    @Test
    void think_Success() {
        // Given
        Perception perception = createTestPerception(100, 100);
        String jsonResponse = """
                {
                  "actions": [
                    {
                      "actionType": "MOVE_TO_CREATURE",
                      "targetIndex": 0,
                      "qualifier": "NEAREST",
                      "actionSummary": "Moving to center"
                    }
                  ]
                }
                """;
        
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult().getOutput().getText()).thenReturn(jsonResponse);

        // When
        Thought result = adapter.think(perception);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.actions()).hasSize(1);
        assertThat(result.actions().get(0).actionType()).isEqualTo(ActionType.MOVE_TO_CREATURE.name());
        assertThat(result.actions().get(0).actionSummary()).isEqualTo("Moving to center");
    }

    @Test
    void think_ApiThrowsException_ReturnsFallback() {
        // Given
        Perception perception = createTestPerception(100, 100);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API Timeout"));

        // When
        Thought result = adapter.think(perception);

        // Then
        assertThat(result.actions()).hasSize(1);
        assertThat(result.actions().get(0).actionType()).isEqualTo(ActionType.IDLE.name());
        assertThat(result.actions().get(0).actionSummary()).contains("AI thinking failed");
        assertThat(result.actions().get(0).actionSummary()).contains("API Timeout");
    }

    @Test
    void think_MalformedJson_ReturnsFallback() {
        // Given
        Perception perception = createTestPerception(100, 100);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult().getOutput().getText()).thenReturn("Invalid JSON");

        // When
        Thought result = adapter.think(perception);

        // Then
        assertThat(result.actions()).hasSize(1);
        assertThat(result.actions().get(0).actionType()).isEqualTo(ActionType.IDLE.name());
        assertThat(result.actions().get(0).actionSummary()).contains("AI had trouble thinking");
    }

    private Perception createTestPerception(int width, int height) {
        return Perception.builder()
                .name("TestAgent")
                .x(0).y(0)
                .mapWidth(width)
                .mapHeight(height)
                .locationName("TestLand")
                .locationDescription("A test environment")
                .currentGoal("Test")
                .lastActionDescription("Nothing")
                .nearbyObjects(Collections.emptyList())
                .build();
    }
}
