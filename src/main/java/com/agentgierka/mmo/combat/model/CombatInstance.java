package com.agentgierka.mmo.combat.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Entity representing a stateful turn-based combat instance between an Agent and a Creature.
 * Uses an Action Point (AP) tick system to determine turn order.
 */
@Entity
@Table(name = "combat_instances")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CombatInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID agentId;

    @Column(nullable = false)
    private UUID creatureInstanceId;

    @Version
    private Long version;

    /** Accumulated points for the Agent. Reach 100 to take a turn. */
    @Builder.Default
    @Column(name = "agent_ap")
    private int agentAp = 0;

    /** Accumulated points for the Creature. Reach 100 to take a turn. */
    @Builder.Default
    @Column(name = "creature_ap")
    private int creatureAp = 0;

    @Builder.Default
    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CombatStatus status = CombatStatus.ONGOING;

    /**
     * Creates a new combat instance between an agent and a creature.
     */
    public static CombatInstance create(UUID agentId, UUID creatureInstanceId) {
        return CombatInstance.builder()
                .agentId(agentId)
                .creatureInstanceId(creatureInstanceId)
                .status(CombatStatus.ONGOING)
                .build();
    }

    /**
     * Accumulates Action Points (AP) for both combatants.
     */
    public void applyTick(int agentSpeed, int creatureSpeed) {
        if (this.status != CombatStatus.ONGOING) {
            return;
        }
        if (agentSpeed <= 0 && creatureSpeed <= 0) {
            throw new IllegalArgumentException("Both combatants cannot have zero or negative speed");
        }
        this.agentAp += agentSpeed;
        this.creatureAp += creatureSpeed;
    }

    /**
     * Checks if the agent has enough AP to take a turn.
     */
    public boolean canAgentAct() {
        return this.status == CombatStatus.ONGOING && this.agentAp >= 100;
    }

    /**
     * Checks if the creature has enough AP to take a turn.
     */
    public boolean canCreatureAct() {
        return this.status == CombatStatus.ONGOING && this.creatureAp >= 100;
    }

    /**
     * Consumes AP cost for an agent's turn.
     */
    public void consumeAgentTurn() {
        if (canAgentAct()) {
            this.agentAp -= 100;
        }
    }

    /**
     * Consumes AP cost for a creature's turn.
     */
    public void consumeCreatureTurn() {
        if (canCreatureAct()) {
            this.creatureAp -= 100;
        }
    }

    /**
     * Finishes the combat and marks the end time.
     */
    public void finishCombat() {
        this.status = CombatStatus.FINISHED;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * Abandons the combat and marks the end time.
     */
    public void abandon() {
        this.status = CombatStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
    }
}
