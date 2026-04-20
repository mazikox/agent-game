package com.agentgierka.mmo.combat.service;

import com.agentgierka.mmo.agent.exception.AgentStateException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final WorldStateSynchronizer worldStateSynchronizer;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Initiates a new combat encounter.
     * Validates states of both parties and locks them in IN_COMBAT status.
     */
    @Transactional
    public CombatInstance initiateCombat(UUID agentId, UUID creatureId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        if (agent.getStatus() == AgentStatus.IN_COMBAT) {
            throw new AgentStateException("Agent is already engaged in combat");
        }

        CreatureInstance creature = creatureRepository.findById(creatureId);
        if (creature == null) {
            throw new CreatureNotFoundException(creatureId.toString());
        }

        if (creature.getState() != CreatureState.ALIVE) {
            throw new CombatException("Creature is not available for combat (State: " + creature.getState() + ")");
        }

        // Logic: Distance check could be added here in the future
        
        // Lock both entities
        agent.updateStatus(AgentStatus.IN_COMBAT, "Engaged in combat with " + creature.getName());
        agentRepository.save(agent);
        worldStateSynchronizer.syncMovementAfterCommit(agent);

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
    @Transactional
    public void executeAction(UUID agentId, CombatActionType actionType) {
        CombatInstance combat = combatRepository.findByAgentIdAndStatus(agentId, CombatStatus.ONGOING)
                .orElseThrow(() -> new CombatException("No ongoing combat found for agent"));

        Agent agent = agentRepository.findById(combat.getAgentId())
                .orElseThrow(() -> new CombatException("Agent record missing"));

        CreatureInstance creature = creatureRepository.findById(combat.getCreatureInstanceId());
        if (creature == null) {
            throw new CreatureNotFoundException(combat.getCreatureInstanceId().toString());
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
        worldStateSynchronizer.syncMovementAfterCommit(agent);
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
                int healAmount = 20; 
                agent.heal(healAmount);
                String msg = "Combat: " + agent.getName() + " uses potion and heals for " + healAmount;
                log.info(msg);
                eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
            }
            case FLEE -> {
                if (!combat.canAgentAct()) throw new CombatException("Must wait for turn to flee");
                combat.finishCombat();
                agent.updateStatus(AgentStatus.IDLE, "Fled from combat");
                creature.exitCombat();
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

    private void syncTime(CombatInstance combat, Agent agent, CreatureInstance creature) {
        if (combat.getStatus() != CombatStatus.ONGOING) return;
        
        // Loop ticks until at least one combatant hits 100 AP
        while (!combat.canAgentAct() && !combat.canCreatureAct()) {
            combat.applyTick(agent.getStats().getAttackSpeed(), creature.getAttackSpeed());
        }
    }

    private void handleVictory(CombatInstance combat, Agent agent, CreatureInstance creature) {
        combat.finishCombat();
        agent.updateStatus(AgentStatus.IDLE, "Victory over " + creature.getName());
        agent.gainExperience(creature.getExperienceReward());
        String msg = "Combat Victory: " + agent.getName() + " defeated " + creature.getName();
        log.info(msg);
        eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
        // Loot handling will be implemented in future tasks
    }

    private void handleDefeat(CombatInstance combat, Agent agent, CreatureInstance creature) {
        combat.finishCombat();
        agent.updateStatus(AgentStatus.RESTING, "Sustained heavy injuries from " + creature.getName());
        creature.exitCombat(); // Creature stays since it won
        String msg = "Combat Defeat: " + agent.getName() + " was defeated by " + creature.getName();
        log.info(msg);
        eventPublisher.publishEvent(new CombatLogEvent(agent.getId(), msg));
    }
}
