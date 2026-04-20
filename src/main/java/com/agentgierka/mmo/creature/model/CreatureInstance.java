package com.agentgierka.mmo.creature.model;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;
import java.time.Instant;

@Getter
@Setter(AccessLevel.PROTECTED)
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreatureInstance implements Serializable {
    private UUID instanceId;
    private UUID templateId;
    private UUID spawnPointId;
    private UUID locationId;

    private String name;
    private CreatureRank rank;

    private int x;
    private int y;
    
    private int level;
    
    private int currentHp;
    private int maxHp;
    private int damage;
    private int aggroRadius;
    private int experienceReward;
    private int attackSpeed;

    private CreatureState state;
    
    private Instant diedAt;
    private int respawnSeconds;
    
    private long version;

    public void incrementVersion() {
        this.version++;
    }

    public static class CreatureInstanceBuilder {
        private int level = 1;
        private int currentHp = 1;
        private int maxHp = 1;
        private int aggroRadius = 1;
        private int attackSpeed = 100;
        private CreatureState state = CreatureState.ALIVE;

        public CreatureInstance build() {
            if (this.currentHp > this.maxHp) {
                throw new IllegalArgumentException("currentHp cannot be greater than maxHp");
            }
            if (this.damage < 0) {
                throw new IllegalArgumentException("damage cannot be negative");
            }
            if (this.respawnSeconds < 0) {
                throw new IllegalArgumentException("respawnSeconds cannot be negative");
            }
            if (this.level <= 0) {
                throw new IllegalArgumentException("level must be greater than 0");
            }
            if (this.aggroRadius <= 0) {
                throw new IllegalArgumentException("aggroRadius must be greater than 0");
            }
            if (this.attackSpeed <= 0) {
                throw new IllegalArgumentException("attackSpeed must be greater than 0");
            }
            
            return new CreatureInstance(instanceId, templateId, spawnPointId, locationId, name, rank, x, y, level, currentHp, maxHp, damage, aggroRadius, experienceReward, attackSpeed, state, diedAt, respawnSeconds, 0L);
        }
    }

    public boolean isDead() {
        return state == CreatureState.DEAD;
    }

    public void takeDamage(int amount) {
        if (state == CreatureState.DEAD) {
            return;
        }
        this.currentHp = Math.max(0, this.currentHp - amount);
        if (this.currentHp == 0) {
            this.state = CreatureState.DEAD;
            this.diedAt = Instant.now();
        }
    }

    public void kill() {
        if (this.state == CreatureState.DEAD) return;
        this.currentHp = 0;
        this.state = CreatureState.DEAD;
        this.diedAt = Instant.now();
    }

    public void respawn(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        this.currentHp = this.maxHp;
        this.state = CreatureState.ALIVE;
        this.diedAt = null;
    }

    public void enterCombat() {
        if (this.state == CreatureState.ALIVE) {
            this.state = CreatureState.IN_COMBAT;
        }
    }

    public void exitCombat() {
        if (this.state == CreatureState.IN_COMBAT) {
            this.state = CreatureState.ALIVE;
        }
    }
}
