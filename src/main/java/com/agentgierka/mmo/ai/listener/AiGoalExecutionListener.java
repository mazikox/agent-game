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
            if (agent.hasActiveGoal()) {
                log.info("Agent {} arrived at {}, but still has a goal: '{}'. Triggering next thought process.", 
                         agent.getName(), event.location().getName(), agent.getGoal());
                
                agentThinkingService.processThinking(agent.getId());
            }
        });
    }
}
