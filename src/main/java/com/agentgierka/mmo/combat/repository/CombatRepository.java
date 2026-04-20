package com.agentgierka.mmo.combat.repository;

import com.agentgierka.mmo.combat.model.CombatInstance;
import com.agentgierka.mmo.combat.model.CombatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing CombatInstance persistence.
 */
@Repository
public interface CombatRepository extends JpaRepository<CombatInstance, UUID> {

    /**
     * Finds an ongoing combat for a specific agent.
     * Useful for validation to ensure an agent isn't in multiple fights.
     */
    Optional<CombatInstance> findByAgentIdAndStatus(UUID agentId, CombatStatus status);

    /**
     * Finds an ongoing combat for a specific creature.
     * Prevents multiple agents from attacking the same creature simultaneously.
     */
    Optional<CombatInstance> findByCreatureInstanceIdAndStatus(UUID creatureInstanceId, CombatStatus status);
}
