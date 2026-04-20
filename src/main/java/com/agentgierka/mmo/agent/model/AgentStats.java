package com.agentgierka.mmo.agent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Value Object representing the RPG statistics of an agent.
 * Implements Rich Domain Model principles by encapsulating game logic.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class AgentStats {

    @Column(name = "hp")
    private int hp;

    @Column(name = "max_hp")
    private int maxHp;

    @Column(name = "experience")
    private int experience;

    @Column(name = "level")
    private int level;

    @Column(name = "base_damage")
    private int baseDamage;

    @Column(name = "attack_speed")
    private int attackSpeed;

    /**
     * Creates initial stats for a new agent.
     */
    public static AgentStats createInitial() {
        return AgentStats.builder()
                .hp(100)
                .maxHp(100)
                .experience(0)
                .level(1)
                .baseDamage(15)
                .attackSpeed(100)
                .build();
    }

    public AgentStats takeDamage(int amount) {
        int newHp = Math.max(0, this.hp - amount);
        return this.toBuilder()
                .hp(newHp)
                .build();
    }

    public AgentStats heal(int amount) {
        int newHp = Math.min(this.maxHp, this.hp + amount);
        return this.toBuilder()
                .hp(newHp)
                .build();
    }

    public AgentStats addExperience(int amount) {
        int newExp = this.experience + amount;
        AgentStats updated = this.toBuilder().experience(newExp).build();
        
        while (updated.experience >= updated.getExpThreshold()) {
            updated = updated.levelUp();
        }
        
        return updated;
    }

    private AgentStats levelUp() {
        int nextThreshold = getExpThreshold();
        int newMaxHp = this.maxHp + 20;
        return this.toBuilder()
                .level(this.level + 1)
                .experience(this.experience - nextThreshold)
                .maxHp(newMaxHp)
                .hp(newMaxHp) // Full heal = new max
                .build();
    }

    public int getExpThreshold() {
        return this.level * 100; // Linear progression: 100, 200, 300...
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
