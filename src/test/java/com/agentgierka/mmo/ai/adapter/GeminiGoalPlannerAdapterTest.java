package com.agentgierka.mmo.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiGoalPlannerAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseBehaviorTreeFromJson() throws Exception {
        // given
        String json = """
        {
          "isSimple": false,
          "tree": {
            "type": "SEQUENCE",
            "children": [
              { "type": "FIND_NEAREST_CREATURE" },
              { "type": "MOVE_TO_TARGET" },
              { "type": "ATTACK_TARGET" }
            ]
          }
        }
        """;

        // when
        PlannerResponse response = objectMapper.readValue(json, PlannerResponse.class);

        // then
        assertFalse(response.isSimple());
        assertNotNull(response.getTree());
        assertEquals("SEQUENCE", response.getTree().getType());
        assertEquals(3, response.getTree().getChildren().size());
        assertEquals("FIND_NEAREST_CREATURE", response.getTree().getChildren().get(0).getType());
        assertEquals("MOVE_TO_TARGET", response.getTree().getChildren().get(1).getType());
        assertEquals("ATTACK_TARGET", response.getTree().getChildren().get(2).getType());
    }
}
