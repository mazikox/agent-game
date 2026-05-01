package com.agentgierka.mmo.ai.behaviortree;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.behaviortree.composite.SequenceNode;
import com.agentgierka.mmo.ai.behaviortree.decorator.RepeatUntilNode;
import com.agentgierka.mmo.ai.behaviortree.leaf.AttackTargetAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.FindNearestCreatureAction;
import com.agentgierka.mmo.ai.behaviortree.leaf.MoveToTargetAction;
import com.agentgierka.mmo.combat.service.CombatService;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgressRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BehaviorTreeCombatTest {

    private BehaviorTreeExecutor executor;
    private BehaviorTreeRegistry registry;
    private CreatureInstanceRepository creatureRepository;
    private PortalRepository portalRepository;
    private CombatService combatService;
    private ApplicationEventPublisher eventPublisher;
    private GoalProgressRegistry goalProgressRegistry;

    @BeforeEach
    void setUp() {
        registry = mock(BehaviorTreeRegistry.class);
        creatureRepository = mock(CreatureInstanceRepository.class);
        portalRepository = mock(PortalRepository.class);
        combatService = mock(CombatService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        goalProgressRegistry = mock(GoalProgressRegistry.class);
        
        executor = new BehaviorTreeExecutor(registry, creatureRepository, portalRepository, combatService, eventPublisher, goalProgressRegistry);
        org.springframework.test.util.ReflectionTestUtils.setField(executor, "maxTicks", 100);
    }

    @Test
    void shouldExecuteFindMoveAttackSequence() {
        // Given: Agent in a location
        Location location = mock(Location.class);
        when(location.getId()).thenReturn(UUID.randomUUID());
        
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.create("Shadow", null, location, 10, 10, 5);
        org.springframework.test.util.ReflectionTestUtils.setField(agent, "id", agentId);
        
        agent.assignGoal("Kill monsters");

        // Given: A creature nearby
        UUID creatureId = UUID.randomUUID();
        CreatureInstance creature = CreatureInstance.builder()
                .instanceId(creatureId)
                .name("Goblin")
                .x(20).y(20)
                .state(CreatureState.ALIVE)
                .build();

        // Given: Behavior Tree [RepeatUntil -> Sequence -> [Find, Move, Attack]]
        BehaviorNode find = new FindNearestCreatureAction();
        BehaviorNode move = new MoveToTargetAction();
        BehaviorNode attack = new AttackTargetAction();
        BehaviorNode sequence = new SequenceNode(List.of(find, move, attack));
        BehaviorNode root = new RepeatUntilNode((progress, a) -> false, sequence);

        when(registry.get(agentId)).thenReturn(Optional.of(root));
        when(creatureRepository.findAllByLocationId(location.getId())).thenReturn(List.of(creature));
        when(creatureRepository.findById(creatureId)).thenReturn(creature);

        // --- TICK 1: FindNearestCreature should target the goblin ---
        executor.tick(agent);
        
        assertEquals(creatureId, agent.getTargetId());
        assertEquals(AgentStatus.MOVING, agent.getStatus(), "Agent should start moving towards target");
        assertEquals(20, agent.getTargetX());
        assertEquals(20, agent.getTargetY());

        // --- Simulate: Agent reaches the target ---
        agent.completeMovement(20, 20);
        
        // --- TICK 2: MoveToTarget should succeed, AttackTarget should initiate combat ---
        executor.tick(agent);
        
        verify(combatService, times(1)).initiateCombat(agentId, creatureId);
        assertEquals(AgentStatus.IN_COMBAT, agent.getStatus());
        assertTrue(agent.hasActiveGoal(), "Goal should still be active during combat");

        // --- Simulate: Creature is killed (e.g. by player or auto-combat) ---
        creature.kill();
        
        // --- TICK 3: AttackTarget should see dead creature and return SUCCESS. Sequence loops. ---
        executor.tick(agent);
        
        assertNull(agent.getTargetId(), "Target should be cleared after victory");
        
        // --- TICK 4: RepeatUntil loops and calls FindNearestCreature again ---
        // Since the creature is dead, find should now fail (unless there's another one)
        when(creatureRepository.findAllByLocationId(location.getId())).thenReturn(List.of(creature)); 
        // Note: FindNearestCreature filters out DEAD creatures
        
        executor.tick(agent);
        
        assertFalse(agent.hasActiveGoal(), "Goal should be cleared when no more creatures are found");
    }
}
