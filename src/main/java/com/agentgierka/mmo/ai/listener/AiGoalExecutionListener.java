package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.agent.event.AgentArrivedAtWaypointEvent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.ai.service.AgentThinkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AiGoalExecutionListener {

    private final AgentRepository agentRepository;
    private final AgentThinkingService agentThinkingService;



    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreatureKilled(CreatureKilledEvent event) {
        if (event.killerId() != null) {
            log.info("Killer confirmed! Waking up AI for agent {}...", event.killerId());
            agentThinkingService.processThinking(event.killerId());
            return;
        }

        agentRepository.findByCurrentLocationId(event.locationId()).forEach(agent -> {
            if (agent.hasActiveGoal() && event.instanceId().equals(agent.getTargetId())) {
                log.info("Targeted creature defeated by unknown force! Waking up AI for agent {}...", agent.getName());
                agentThinkingService.processThinking(agent.getId());
            }
        });
    }

    @EventListener
    public void onAgentArrived(AgentArrivedAtWaypointEvent event) {
        agentRepository.findById(event.agentId()).ifPresent(agent -> {
            if (agent.hasActiveGoal()) {
                log.info("Agent {} arrived at {} (Type: {}). Ticking Behavior Tree...", 
                         agent.getName(), event.location().getName(), event.type());
                agentThinkingService.processThinking(agent.getId());
            } else {
                log.info("MISSION COMPLETE: Agent {} arrived at {} (Type: {}). Standing by for new commands.", 
                         agent.getName(), event.location().getName(), event.type());
            }
        });
    }
}
