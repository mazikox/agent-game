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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class AgentStats {

    @Column(name = "hp")
    private Integer hp;

    @Column(name = "max_hp")
    private Integer maxHp;

    @Column(name = "experience")
    private Integer experience;

    @Column(name = "level")
    private Integer level;

    /**
     * Creates initial stats for a new agent.
     */
    public static AgentStats createInitial() {
        return AgentStats.builder()
                .hp(100)
                .maxHp(100)
                .experience(0)
                .level(1)
                .build();
    }

    /**
     * Logic for taking damage.
     * @return true if still alive, false if died.
     */
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
        return this.toBuilder()
                .level(this.level + 1)
                .experience(this.experience - nextThreshold)
                .maxHp(this.maxHp + 20)
                .hp(this.maxHp + 20) // Full heal on level up
                .build();
    }

    public int getExpThreshold() {
        return this.level * 100; // Linear progression: 100, 200, 300...
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
