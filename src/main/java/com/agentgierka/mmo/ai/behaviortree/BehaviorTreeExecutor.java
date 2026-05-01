package com.agentgierka.mmo.ai.behaviortree;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.combat.service.CombatService;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgress;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgressRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BehaviorTreeExecutor {

    private final BehaviorTreeRegistry registry;
    private final CreatureInstanceRepository creatureRepository;
    private final PortalRepository portalRepository;
    private final CombatService combatService;
    private final ApplicationEventPublisher eventPublisher;
    private final GoalProgressRegistry goalProgressRegistry;

    @Value("${game.behavior-tree.max-ticks:100}")
    private int maxTicks;

    private final Cache<UUID, Integer> tickCounters = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    public void tick(Agent agent) {
        BehaviorNode tree = registry.get(agent.getId()).orElse(null);
        if (tree == null) {
            log.warn("No active behavior tree found for agent {}", agent.getName());
            return;
        }

        int currentTicks = tickCounters.asMap().merge(agent.getId(), 1, Integer::sum);
        if (currentTicks > maxTicks) {
            log.warn("Agent {} exceeded max behavior tree ticks ({}). Aborting.", agent.getName(), maxTicks);
            cleanUp(agent.getId());
            agent.clearGoal();
            return;
        }

        GoalProgress progress = goalProgressRegistry.getOrCreate(agent.getId());
        BehaviorContext context = new BehaviorContext(
                agent, creatureRepository, portalRepository, combatService, eventPublisher, progress
        );

        try {
            log.info("Tick #{} for agent {} Behavior Tree: {}", currentTicks, agent.getName(), tree.describe());
            NodeStatus status = tree.tick(context);

            if (status == NodeStatus.SUCCESS || status == NodeStatus.FAILURE) {
                log.info("Agent {} behavior tree finished with status: {}", agent.getName(), status);
                cleanUp(agent.getId());
                agent.clearGoal();
            }

        } catch (Exception e) {
            log.error("Unexpected error during Behavior Tree execution for agent {}: ", agent.getName(), e);
            cleanUp(agent.getId());
            agent.clearGoal();
        }
    }

    private void cleanUp(UUID agentId) {
        registry.remove(agentId);
        goalProgressRegistry.remove(agentId);
        tickCounters.invalidate(agentId);
    }
}
