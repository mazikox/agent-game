package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.agent.event.AgentGoalCompletedEvent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.ai.service.AgentThinkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AiGoalExecutionListener {

    private final AgentRepository agentRepository;
    private final AgentThinkingService agentThinkingService;

    @EventListener
    public void onAgentGoalCompleted(AgentGoalCompletedEvent event) {
        agentRepository.findById(event.agentId()).ifPresent(agent -> {
            if (agent.hasActiveGoal()) {
                log.info("Agent {} reached waypoint at {}. Ticking Behavior Tree...", 
                         agent.getName(), event.location().getName());
                agentThinkingService.processThinking(agent.getId());
            } else {
                log.info("MISSION COMPLETE: Agent {} reached destination at {}. Standing by for new commands.", 
                         agent.getName(), event.location().getName());
            }
        });
    }
}
