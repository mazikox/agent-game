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
    
    private int currentHp;
    private int maxHp;
    private int damage;
    private int aggroRadius;
    private int experienceReward;

    private CreatureState state;
    
    private Instant diedAt;
    private int respawnSeconds;

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
}
