package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgress;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgressRegistry;
import com.agentgierka.mmo.agent.event.AgentArrivedAtWaypointEvent;
import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.creature.model.CreatureRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Listener that updates GoalProgress based on game events.
 * It connects the world events with the AI goal tracking state.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class GoalProgressListener {

    private final GoalProgressRegistry goalProgressRegistry;

    @EventListener
    public void onCreatureKilled(CreatureKilledEvent event) {
        if (event.killerId() == null) {
            return;
        }

        GoalProgress progress = goalProgressRegistry.getIfExists(event.killerId());
        if (progress == null) {
            // Agent doesn't have an active goal progress tracked
            return;
        }

        log.debug("Updating GoalProgress for agent {}: +1 kill, +{} exp", event.killerId(), event.expReward());
        
        progress.incrementKills();
        progress.addExpGained(event.expReward());
        
        if (event.drops() != null) {
            progress.addItemsCollected(event.drops().size());
        }

        if (event.rank() == CreatureRank.BOSS) {
            progress.incrementBossKills();
        } else if (event.rank() == CreatureRank.ELITE || event.rank() == CreatureRank.RARE) {
            progress.incrementEliteKills();
        }
    }

    @EventListener
    public void onAgentArrived(AgentArrivedAtWaypointEvent event) {
        GoalProgress progress = goalProgressRegistry.getIfExists(event.agentId());
        if (progress != null && event.location() != null) {
            progress.addLocationVisited(event.location().getName());
        }
    }
}
