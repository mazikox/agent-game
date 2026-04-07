package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
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
    public void onAgentArrived(AgentArrivedEvent event) {
        agentRepository.findById(event.agentId()).ifPresent(agent -> {
            if (agent.getRemainingThinkingSteps() != null && agent.getRemainingThinkingSteps() <= 0) {
                log.info("MISSION COMPLETE: Agent {} reached destination at {}. Standing by for new commands.", 
                         agent.getName(), event.location().getName());
            } else if (agent.hasActiveGoal()) {
                // If we ever want to re-enable autonomy, we would call processThinking here.
                // For now, in manual mode, we just log that we are pausing.
                log.info("Agent {} reached waypoint at {}, but manual mode is active. Pausing.", 
                         agent.getName(), event.location().getName());
            }
        });
    }
}
