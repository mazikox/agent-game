package com.agentgierka.mmo.combat.service;

import com.agentgierka.mmo.agent.exception.AgentStateException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.agentgierka.mmo.combat.exception.CombatException;
import com.agentgierka.mmo.combat.model.CombatActionType;
import com.agentgierka.mmo.combat.model.CombatInstance;
import com.agentgierka.mmo.combat.model.CombatStatus;
import com.agentgierka.mmo.combat.repository.CombatRepository;
import com.agentgierka.mmo.combat.event.CombatLogEvent;
import com.agentgierka.mmo.creature.exception.CreatureNotFoundException;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.service.SpawnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

import java.util.UUID;

/**
 * Service orchestrating combat mechanics between Agents and Creatures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CombatService {

    private final CombatRepository combatRepository;
    private final AgentRepository agentRepository;
    private final CreatureInstanceRepository creatureRepository;
    private final SpawnService spawnService;
    private final WorldStateSynchronizer worldStateSynchronizer;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentWorldStateRepository agentWorldStateRepository;

    private int potionHealAmount = 20;

    /**
     * Initiates a new combat encounter.
     * Validates states of both parties and locks them in IN_COMBAT status.
     */
    @Transactional
    public CombatInstance initiateCombat(UUID agentId, UUID creatureId) {
        Agent agent = agentRepository.findByIdForUpdate(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        if (agent.getStatus() == AgentStatus.IN_COMBAT) {
            throw new AgentStateException("Agent is already engaged in combat");
        }

        combatRepository.findByAgentIdAndStatus(agentId, CombatStatus.ONGOING)
                .ifPresent(existing -> {
                    throw new CombatException("Agent already has an ongoing combat instance");
                });

        CreatureInstance creature = creatureRepository.findById(creatureId);
        if (creature == null) {
            throw new CreatureNotFoundException(creatureId.toString());
        }

        if (creature.getState() != CreatureState.ALIVE) {
            throw new CombatException("Creature is not available for combat (State: " + creature.getState() + ")");
        }

        combatRepository.findByCreatureInstanceIdAndStatus(creatureId, CombatStatus.ONGOING)
                .ifPresent(existing -> {
                    throw new CombatException("Creature is already engaged in combat");
                });

        // Logic: Distance check could be added here in the future
        
        // Lock both entities
        agent.updateStatus(AgentStatus.IN_COMBAT, "Engaged in combat with " + creature.getName());
        agent.targetEntity(creatureId);

        // Keep the agent's real-time position from Redis if they were moving
        AgentWorldState existing = agentWorldStateRepository.findById(agentId);
        if (existing != null && existing.getStatus() == AgentStatus.MOVING) {
            AgentWorldState updated = existing.toBuilder()
                .status(AgentStatus.IN_COMBAT)
                .targetId(creature.getInstanceId())
                .build();
            agentWorldStateRepository.save(updated);
            agent.syncWithWorldState(existing);
        } else {
            worldStateSynchronizer.syncToRedis(agent);
        }

        agentRepository.save(agent);
        worldStateSynchronizer.publishStatusChangedEvent(agent);
        worldStateSynchronizer.publishCombatTargetChangedEvent(agent, creature);

        creature.enterCombat();
        boolean locked = creatureRepository.updateAtomic(creature);
        if (!locked) {
            throw new CombatException("Failed to lock creature for combat - possible race condition");
        }

        CombatInstance combat = CombatInstance.create(agentId, creatureId);
        
        // Initial time sync to prepare first turns
        syncTime(combat, agent, creature);
        
        String msg = "Combat initiated: " + agent.getName() + " vs " + creature.getName();
        log.info(msg);
        eventPublisher.publishEvent(new CombatLogEvent(agentId, msg));
        return combatRepository.save(combat);
    }

    /**
     * Executes an action in an ongoing combat.
     */
    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = 100
    )
    @Transactional
    public void executeAction(UUID agentId, CombatActionType actionType) {
        var combatOpt = combatRepository.findByAgentIdAndStatus(agentId, CombatStatus.ONGOING);

        if (combatOpt.isEmpty()) {
            log.warn("executeAction called but no ongoing combat for agent {}. Ignoring (possible duplicate request).", agentId);
            return;
        }

        CombatInstance combat = combatOpt.get();

        Agent agent = agentRepository.findById(combat.getAgentId())
                .orElseThrow(() -> new CombatException("Agent record missing"));

        CreatureInstance creature = creatureRepository.findById(combat.getCreatureInstanceId());
        if (creature == null) {
            log.warn("Creature {} not found for active combat {}. Abandoning combat.", combat.getCreatureInstanceId(), combat.getId());
            combat.abandon();
            combatRepository.save(combat);
            agent.updateStatus(AgentStatus.IDLE, "Combat abandoned (creature disappeared)");
            agent.clearTarget();
            agentRepository.save(agent);
            worldStateSynchronizer.syncToRedis(agent);
            worldStateSynchronizer.publishStatusChangedEvent(agent);
            worldStateSynchronizer.publishCombatTargetChangedEvent(agent, null);
            return;
        }

        // 1. Process Agent's Action
        resolveAgentAction(combat, agent, creature, actionType);

        // 2. Immediate cleanup if creature is dead
        if (creature.isDead()) {
            handleVictory(combat, agent, creature);
        } else {
            // 3. Auto-process Monster Turns (Creature hits as long as it has AP)
            resolveCreatureTurns(combat, agent, creature);

            if (!agent.getStats().isAlive()) {
                handleDefeat(combat, agent, creature);
            } else {
                // 4. Advance Time until someone has 100 AP
                syncTime(combat, agent, creature);
            }
        }

        // 5. Persist all changes
        agentRepository.save(agent);
        worldStateSynchronizer.syncToRedis(agent);
        
        // Publish granular WebSocket updates for the new typed events system
        worldStateSynchronizer.publishHealthChangedEvent(agent);
        if (combat.getStatus() == CombatStatus.ONGOING && !creature.isDead()) {
            worldStateSynchronizer.publishCombatTargetChangedEvent(agent, creature);
        }
        
        creatureRepository.updateAtomic(creature);
        combatRepository.save(combat);
    }

    private void resolveAgentAction(CombatInstance combat, Agent agent, CreatureInstance creature, CombatActionType actionType) {
        switch (actionType) {
            case ATTACK -> {
                if (!combat.canAgentAct()) throw new CombatException("Must wait for turn to attack");
                int damage = agent.getStats().getBaseDamage();
                creature.takeDamage(damage);
                combat.consumeAgentTurn();
                String msg = "Combat: " + agent.getName() + " attacks " + creature.getName() + " for " + damage + " damage";
                log.info(msg);
                eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
            }
            case POTION -> {
                if (!combat.canAgentAct()) throw new CombatException("Must wait for turn to use potion");
                agent.heal(potionHealAmount);
                combat.consumeAgentTurn();
                String msg = "Combat: " + agent.getName() + " uses potion and heals for " + potionHealAmount;
                log.info(msg);
                eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
            }
            case FLEE -> {
                if (!combat.canAgentAct()) throw new CombatException("Must wait for turn to flee");
                combat.finishCombat();
                agent.updateStatus(AgentStatus.IDLE, "Fled from combat");
                agent.clearTarget();
                creature.exitCombat();
                worldStateSynchronizer.publishStatusChangedEvent(agent);
                worldStateSynchronizer.publishCombatTargetChangedEvent(agent, null);
                String msg = "Combat: " + agent.getName() + " fled from " + creature.getName();
                log.info(msg);
                eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
            }
            default -> throw new UnsupportedOperationException("Action not implemented yet: " + actionType);
        }
    }

    private void resolveCreatureTurns(CombatInstance combat, Agent agent, CreatureInstance creature) {
        while (combat.canCreatureAct() && agent.getStats().isAlive()) {
            int damage = creature.getDamage();
            agent.takeDamage(damage);
            combat.consumeCreatureTurn();
            String msg = "Combat: " + creature.getName() + " counter-attacks " + agent.getName() + " for " + damage + " damage";
            log.info(msg);
            eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
        }
    }

    private static final int MAX_TICKS = 10_000;

    private void syncTime(CombatInstance combat, Agent agent, CreatureInstance creature) {
        if (combat.getStatus() != CombatStatus.ONGOING) return;
        
        int ticks = 0;
        // Loop ticks until at least one combatant hits 100 AP
        while (!combat.canAgentAct() && !combat.canCreatureAct()) {
            if (++ticks > MAX_TICKS) {
                log.error("syncTime exceeded max ticks — agentSpeed={}, creatureSpeed={}",
                    agent.getStats().getAttackSpeed(), creature.getAttackSpeed());
                combat.abandon();
                throw new CombatException("Combat stalled — possible zero-speed configuration");
            }
            combat.applyTick(agent.getStats().getAttackSpeed(), creature.getAttackSpeed());
        }
    }

    private void handleVictory(CombatInstance combat, Agent agent, CreatureInstance creature) {
        combat.finishCombat();
        agent.updateStatus(AgentStatus.IDLE, "Victory over " + creature.getName());
        agent.clearTarget();
        agent.gainExperience(creature.getExperienceReward());
        
        worldStateSynchronizer.publishStatusChangedEvent(agent);
        worldStateSynchronizer.publishCombatTargetChangedEvent(agent, null);
        
        // Trigger death ceremony (WebSocket events, loot)
        spawnService.killCreature(creature.getInstanceId(), agent.getId());
        
        String msg = "Combat Victory: " + agent.getName() + " defeated " + creature.getName();
        log.info(msg);
        eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
    }

    private void handleDefeat(CombatInstance combat, Agent agent, CreatureInstance creature) {
        combat.finishCombat();
        agent.updateStatus(AgentStatus.RESTING, "Sustained heavy injuries from " + creature.getName());
        agent.clearTarget();
        creature.exitCombat(); // Creature stays since it won
        
        worldStateSynchronizer.publishStatusChangedEvent(agent);
        worldStateSynchronizer.publishCombatTargetChangedEvent(agent, null);
        
        String msg = "Combat Defeat: " + agent.getName() + " was defeated by " + creature.getName();
        log.info(msg);
        eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
    }
}
