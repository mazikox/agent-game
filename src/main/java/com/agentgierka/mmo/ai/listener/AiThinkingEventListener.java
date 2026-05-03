package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.ai.event.AiThinkingRequiredEvent;
import com.agentgierka.mmo.ai.service.AgentThinkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener that handles asynchronous AI thinking requests.
 * By using @Async, we break the call stack and prevent stack overflow 
 * during rapid AI state transitions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiThinkingEventListener {

    private final AgentThinkingService agentThinkingService;

    @Async("taskExecutor")
    @EventListener
    public void onAiThinkingRequired(AiThinkingRequiredEvent event) {
        log.debug("Processing asynchronous thinking request for agent {}", event.agentId());
        agentThinkingService.processThinking(event.agentId());
    }
}
