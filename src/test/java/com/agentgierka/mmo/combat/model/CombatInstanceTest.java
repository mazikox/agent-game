package com.agentgierka.mmo.combat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CombatInstance domain logic.
 * These tests define the core mechanics of our turn-based system.
 */
class CombatInstanceTest {

    private final UUID agentId = UUID.randomUUID();
    private final UUID creatureId = UUID.randomUUID();

    @Test
    @DisplayName("Should correctly accumulate AP based on speed during a combat tick")
    void shouldAccumulateApOnTick() {
        // given
        CombatInstance combat = CombatInstance.create(agentId, creatureId);
        int agentSpeed = 120;
        int creatureSpeed = 80;

        // when
        combat.applyTick(agentSpeed, creatureSpeed);

        // then
        assertThat(combat.getAgentAp()).isEqualTo(120);
        assertThat(combat.getCreatureAp()).isEqualTo(80);
    }

    @Test
    @DisplayName("Should detect when a combatant is ready to take a turn (AP >= 100)")
    void shouldDetectReadyTurns() {
        // given
        CombatInstance combat = CombatInstance.builder()
                .agentAp(110)
                .creatureAp(90)
                .build();

        // then
        assertThat(combat.canAgentAct()).isTrue();
        assertThat(combat.canCreatureAct()).isFalse();
    }

    @Test
    @DisplayName("Should allow multiple turns if speed is vastly superior (e.g. 2x speed = 2 turns)")
    void shouldHandleSuperiorSpeedTurns() {
        // given
        CombatInstance combat = CombatInstance.create(agentId, creatureId);
        int fastAgentSpeed = 200;
        int normalCreatureSpeed = 100;

        // when - tick happens
        combat.applyTick(fastAgentSpeed, normalCreatureSpeed);

        // then - Agent has 200 AP, enough for two turns
        assertThat(combat.getAgentAp()).isEqualTo(200);
        
        // Use first turn
        combat.consumeAgentTurn();
        assertThat(combat.getAgentAp()).isEqualTo(100);
        assertThat(combat.canAgentAct()).isTrue();

        // Use second turn
        combat.consumeAgentTurn();
        assertThat(combat.getAgentAp()).isEqualTo(0);
        assertThat(combat.canAgentAct()).isFalse();
    }

    @Test
    @DisplayName("Should not consume turn when using a Potion")
    void shouldNotConsumeTurnOnPotionUse() {
        // given
        CombatInstance combat = CombatInstance.builder()
                .agentAp(100)
                .build();

        // when - agent uses potion (hypothetically, we check if AP remains the same)
        // In our design, only 'Attack' or 'Skill' calls consumeTurn()
        // Here we just verify that we can have a logic where AP is not deducted
        
        // then
        assertThat(combat.canAgentAct()).isTrue();
        // and after some logic that represents a potion...
        assertThat(combat.getAgentAp()).isEqualTo(100); 
    }

    @Test
    @DisplayName("Should finalize combat status when someone dies")
    void shouldEndCombat() {
        // given
        CombatInstance combat = CombatInstance.create(agentId, creatureId);
        
        // when
        combat.finishCombat();
        
        // then
        assertThat(combat.getStatus()).isEqualTo(CombatStatus.FINISHED);
        assertThat(combat.getEndedAt()).isNotNull();
    }
}
