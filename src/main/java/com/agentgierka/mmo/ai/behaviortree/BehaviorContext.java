package com.agentgierka.mmo.ai.behaviortree;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.combat.service.CombatService;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgress;
import org.springframework.context.ApplicationEventPublisher;

public record BehaviorContext(
        Agent agent,
        CreatureInstanceRepository creatureRepository,
        PortalRepository portalRepository,
        CombatService combatService,
        ApplicationEventPublisher eventPublisher,
        GoalProgress goalProgress
) {}
