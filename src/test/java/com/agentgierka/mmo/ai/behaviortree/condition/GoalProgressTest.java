package com.agentgierka.mmo.ai.behaviortree.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoalProgressTest {

    @Test
    @DisplayName("Should correctly track kills and boss kills")
    void shouldTrackKills() {
        GoalProgress progress = new GoalProgress();

        progress.incrementKills();
        progress.incrementKills();
        progress.incrementBossKills();

        assertEquals(2, progress.getKillCount());
        assertEquals(1, progress.getBossKills());
        assertEquals(0, progress.getEliteKills());
    }

    @Test
    @DisplayName("Should correctly track exp and items")
    void shouldTrackExpAndItems() {
        GoalProgress progress = new GoalProgress();

        progress.addExpGained(100);
        progress.addExpGained(50);
        progress.addItemsCollected(5);

        assertEquals(150, progress.getExpGained());
        assertEquals(5, progress.getItemsCollected());
    }

    @Test
    @DisplayName("Should track unique locations visited")
    void shouldTrackUniqueLocations() {
        GoalProgress progress = new GoalProgress();

        progress.addLocationVisited("Forest");
        progress.addLocationVisited("Forest");
        progress.addLocationVisited("Cave");

        assertEquals(2, progress.getLocationsVisitedCount());
        assertTrue(progress.getLocationsVisited().contains("Forest"));
        assertTrue(progress.getLocationsVisited().contains("Cave"));
    }
}
