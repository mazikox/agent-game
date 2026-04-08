package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.agent.event.GoalAssignedEvent;
import com.agentgierka.mmo.ai.service.AgentThinkingService;
import com.agentgierka.mmo.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ensures AI thinking runs after the goal assignment commits,
 * so the DB connection is not held open during the (slow) HTTP call to the AI
 * provider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoalAssignedListener {

    private final AgentThinkingService agentThinkingService;
    private final AgentService agentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGoalAssigned(GoalAssignedEvent event) {
        var agent = agentService.findById(event.agentId());
        log.info("Goal assigned event received for agent {}. Starting AI thinking...", agent.getName());
        agentThinkingService.processThinking(event.agentId());
    }
}
