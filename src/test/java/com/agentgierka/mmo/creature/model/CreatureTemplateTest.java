package com.agentgierka.mmo.creature.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CreatureTemplateTest {

    @Test
    void shouldCalculateScaledStatsForNormalRank() {
        CreatureTemplate wolf = CreatureTemplate.create("Wolf", CreatureRank.NORMAL, 1, 100, 10, 10, 5);

        assertThat(wolf.getScaledHp()).isEqualTo(120); // 100 * 1.0 + 1 * 20
        assertThat(wolf.getScaledDamage()).isEqualTo(12); // 10 * 1.0 + 1 * 2
    }

    @Test
    void shouldCalculateScaledStatsForBossRank() {
        CreatureTemplate dragon = CreatureTemplate.create("Dragon", CreatureRank.BOSS, 10, 1000, 100, 500, 15);

        assertThat(dragon.getScaledHp()).isEqualTo(5200); // 1000 * 5.0 + 10 * 20
        assertThat(dragon.getScaledDamage()).isEqualTo(520); // 100 * 5.0 + 10 * 2
        assertThat(dragon.getScaledExperience()).isEqualTo(2550); // 500 * 5.0 + 10 * 5
    }

    @Test
    void shouldHandleHighLevelScaling() {
        CreatureTemplate legendary = CreatureTemplate.create("Legend", CreatureRank.ELITE, 100, 1000, 100, 200, 20);

        // HP: 1000 * 2.0 + 100 * 20 = 4000
        assertThat(legendary.getScaledHp()).isEqualTo(4000);
        // XP: 200 * 2.0 + 100 * 5 = 900
        assertThat(legendary.getScaledExperience()).isEqualTo(900);
    }
}
