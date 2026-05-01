package com.agentgierka.mmo.ai.behaviortree.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GoalProgressRegistryTest {

    @Test
    @DisplayName("Should create and reuse GoalProgress for same agent")
    void shouldReuseProgress() {
        GoalProgressRegistry registry = new GoalProgressRegistry();
        UUID agentId = UUID.randomUUID();

        GoalProgress first = registry.getOrCreate(agentId);
        GoalProgress second = registry.getOrCreate(agentId);

        assertNotNull(first);
        assertSame(first, second, "Should return the same instance for the same agentId");
    }

    @Test
    @DisplayName("Should return null if progress doesn't exist and using getIfExists")
    void shouldReturnNullIfNotExists() {
        GoalProgressRegistry registry = new GoalProgressRegistry();
        UUID agentId = UUID.randomUUID();

        assertNull(registry.getIfExists(agentId));
    }

    @Test
    @DisplayName("Should remove progress")
    void shouldRemoveProgress() {
        GoalProgressRegistry registry = new GoalProgressRegistry();
        UUID agentId = UUID.randomUUID();

        registry.getOrCreate(agentId);
        assertNotNull(registry.getIfExists(agentId));

        registry.remove(agentId);
        assertNull(registry.getIfExists(agentId));
    }
}
