package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.agent.event.AgentGoalCompletedEvent;
import com.agentgierka.mmo.agent.model.GoalExecutionMode;
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
            if (agent.getExecutionMode() == GoalExecutionMode.BEHAVIOR_TREE && agent.hasActiveGoal()) {
                log.info("Agent {} reached waypoint. Ticking Behavior Tree...", agent.getName());
                agentThinkingService.processThinking(agent.getId());
            } else if (agent.getRemainingThinkingSteps() != null && agent.getRemainingThinkingSteps() <= 0 && !agent.hasActions()) {
                log.info("MISSION COMPLETE: Agent {} reached destination at {}. Standing by for new commands.", 
                         agent.getName(), event.location().getName());
            } else if (agent.hasActions()) {
                log.info("Agent {} reached waypoint at {}. Proceeding to next action in queue...", 
                         agent.getName(), event.location().getName());
                agentThinkingService.executeNextActionStep(agent);
                agentRepository.save(agent);
            } else if (agent.hasActiveGoal()) {
                log.info("Agent {} reached waypoint at {}, plan is complete but goal '{}' is still active.", 
                         agent.getName(), event.location().getName(), agent.getGoal());
            }
        });
    }
}
