package com.agentgierka.mmo.creature.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class CreatureInstanceTest {

    @Test
    void shouldDecreaseHpWhenTakingDamage() {
        CreatureInstance instance = CreatureInstance.builder()
                .currentHp(100)
                .state(CreatureState.ALIVE)
                .build();

        instance.takeDamage(30);

        assertThat(instance.getCurrentHp()).isEqualTo(70);
        assertThat(instance.getState()).isEqualTo(CreatureState.ALIVE);
    }

    @Test
    void shouldDieWhenHpReachesZero() {
        CreatureInstance instance = CreatureInstance.builder()
                .currentHp(50)
                .state(CreatureState.ALIVE)
                .build();

        instance.takeDamage(50);

        assertThat(instance.getCurrentHp()).isZero();
        assertThat(instance.getState()).isEqualTo(CreatureState.DEAD);
        assertThat(instance.getDiedAt()).isNotNull();
    }

    @Test
    void shouldClampHpAtZero() {
        CreatureInstance instance = CreatureInstance.builder()
                .currentHp(10)
                .state(CreatureState.ALIVE)
                .build();

        instance.takeDamage(100);

        assertThat(instance.getCurrentHp()).isZero();
        assertThat(instance.getState()).isEqualTo(CreatureState.DEAD);
    }

    @Test
    void shouldNotChangeStateIfAlreadyDead() {
        Instant originalDeathTime = Instant.now().minusSeconds(10);
        CreatureInstance instance = CreatureInstance.builder()
                .currentHp(0)
                .state(CreatureState.DEAD)
                .diedAt(originalDeathTime)
                .build();

        instance.takeDamage(10);

        assertThat(instance.getDiedAt()).isEqualTo(originalDeathTime);
    }
}
