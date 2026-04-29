package com.agentgierka.mmo.ai.behaviortree;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.service.ActionResolverService;
import com.agentgierka.mmo.combat.service.CombatService;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.world.PortalRepository;
import org.springframework.context.ApplicationEventPublisher;

public record BehaviorContext(
        Agent agent,
        ActionResolverService actionResolver,
        CreatureInstanceRepository creatureRepository,
        PortalRepository portalRepository,
        CombatService combatService,
        ApplicationEventPublisher eventPublisher
) {}
