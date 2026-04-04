package com.agentgierka.mmo.agent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentWorldStateTest {

    @Test
    @DisplayName("Should update position correctly within one step")
    void shouldUpdatePositionWithinOneStep() {
        // Given
        AgentWorldState state = AgentWorldState.builder()
                .x(10).y(10)
                .targetX(12).targetY(10)
                .speed(5)
                .build();

        // When
        boolean reached = state.updatePosition();

        // Then
        assertTrue(reached);
        assertEquals(12, state.getX());
        assertEquals(10, state.getY());
    }

    @Test
    @DisplayName("Should move multiple steps to reach target")
    void shouldMoveMultipleSteps() {
        // Given
        AgentWorldState state = AgentWorldState.builder()
                .x(0).y(0)
                .targetX(10).targetY(0)
                .speed(3)
                .build();

        // When/Then
        assertFalse(state.updatePosition()); // Step 1: to (3,0)
        assertEquals(3, state.getX());

        assertFalse(state.updatePosition()); // Step 2: to (6,0)
        assertEquals(6, state.getX());

        assertFalse(state.updatePosition()); // Step 3: to (9,0)
        assertEquals(9, state.getX());

        assertTrue(state.updatePosition());  // Step 4: to (10,0) - reached
        assertEquals(10, state.getX());
    }

    @Test
    @DisplayName("Should move diagonally")
    void shouldMoveDiagonally() {
        // Given
        AgentWorldState state = AgentWorldState.builder()
                .x(0).y(0)
                .targetX(2).targetY(2)
                .speed(1)
                .build();

        // When
        state.updatePosition();

        // Then
        assertEquals(1, state.getX());
        assertEquals(1, state.getY());
        assertFalse(state.isAtTarget());

        state.updatePosition();
        assertTrue(state.isAtTarget());
    }

    @Test
    @DisplayName("Should handle null targets gracefully without NPE")
    void shouldHandleNullTargetsGracefully() {
        // Given
        AgentWorldState state = AgentWorldState.builder()
                .x(10).y(10)
                .targetX(null).targetY(null)
                .build();

        // When/Then
        assertDoesNotThrow(state::updatePosition);
        assertTrue(state.updatePosition());
    }
}
