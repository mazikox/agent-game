package com.agentgierka.mmo.agent.model;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.world.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Agent Domain Unit Tests")
class AgentDomainTest {

    @Test
    @DisplayName("Should prepare perception with all relevant agent state")
    void shouldPreparePerceptionWithAllRelevantAgentState() {
        Location forest = Location.builder().name("Deep Forest").build();
        Agent agent = Agent.builder()
                .name("Gierko")
                .x(10).y(20)
                .currentLocation(forest)
                .goal("Find a portal")
                .currentActionDescription("Resting")
                .build();
        List<String> nearby = List.of("Portal at (15, 15)");

        Perception perception = agent.preparePerception(nearby);

        assertEquals("Gierko", perception.name());
        assertEquals(10, perception.x());
        assertEquals(20, perception.y());
        assertEquals("Deep Forest", perception.locationName());
        assertEquals("Find a portal", perception.currentGoal());
        assertEquals("Resting", perception.lastActionDescription());
        assertTrue(perception.nearbyObjects().contains("Portal at (15, 15)"));
    }

    @Test
    @DisplayName("Should update internal state when applying a thought")
    void shouldUpdateInternalStateWhenApplyingAThought() {
        Agent agent = Agent.builder().name("Gierko").build();
        Thought thought = Thought.builder()
                .targetX(100).targetY(200)
                .status("MOVING")
                .actionSummary("Decided to exploration")
                .nextGoal("Explore new maps")
                .build();

        agent.applyThought(thought);

        assertEquals(100, agent.getTargetX());
        assertEquals(200, agent.getTargetY());
        assertEquals(AgentStatus.MOVING, agent.getStatus());
        assertEquals("Explore new maps", agent.getCurrentTask());
        assertEquals("Decided to exploration", agent.getCurrentActionDescription());
    }

    @Test
    @DisplayName("Should finalize movement and set idle status")
    void shouldFinalizeMovementAndSetIdleStatus() {
        Agent agent = Agent.builder().status(AgentStatus.MOVING).build();

        agent.completeMovement(50, 60);

        assertEquals(50, agent.getX());
        assertEquals(60, agent.getY());
        assertEquals(AgentStatus.IDLE, agent.getStatus());
        assertNull(agent.getTargetX());
        assertTrue(agent.getCurrentActionDescription().contains("(50, 60)"));
    }

    @Test
    @DisplayName("Should take damage and update current action description on death")
    void shouldTakeDamageCorrectly() {
        Agent agent = Agent.builder().name("Shadow")
                .stats(AgentStats.createInitial())
                .build();

        agent.takeDamage(30);
        assertEquals(70, agent.getStats().getHp());

        agent.takeDamage(80);
        assertEquals(0, agent.getStats().getHp());
        assertTrue(agent.getCurrentActionDescription().contains("fallen in battle"));
    }

    @Test
    @DisplayName("Should gain experience and level up automatically")
    void shouldGainExperienceAndLevelUp() {
        Agent agent = Agent.builder().name("Hero")
                .stats(AgentStats.builder().hp(100).maxHp(100).level(1).experience(50).build())
                .build();

        // Poziom 1 próg to 100 EXP. 50 + 60 = 110. Powinien być awans.
        agent.gainExperience(60);

        assertEquals(2, agent.getStats().getLevel());
        assertEquals(10, agent.getStats().getExperience()); 
        assertTrue(agent.getCurrentActionDescription().contains("LEVEL UP"));
    }

    @Test
    @DisplayName("Should heal to max HP but not above")
    void shouldHealCorrectly() {
        Agent agent = Agent.builder()
                .stats(AgentStats.builder().hp(50).maxHp(100).build())
                .build();

        agent.heal(20);
        assertEquals(70, agent.getStats().getHp());

        agent.heal(100);
        assertEquals(100, agent.getStats().getHp());
    }
}
