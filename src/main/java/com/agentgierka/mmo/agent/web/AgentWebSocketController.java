package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
import com.agentgierka.mmo.agent.event.AgentStateUpdatedEvent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Adapter that listens for internal domain events and broadcasts them
 * to subscribed clients via WebSockets (STOMP).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AgentWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onAgentStateUpdated(AgentStateUpdatedEvent event) {
        AgentWorldState state = event.getState();
        String destination = "/topic/agents/" + state.getAgentId();
        
        log.debug("Broadcasting update for agent {} to {}", state.getAgentId(), destination);
        
        messagingTemplate.convertAndSend(destination, state);
    }
    
    @EventListener
    public void onAgentArrived(AgentArrivedEvent event) {
        String destination = "/topic/agents/" + event.agentId();
        
        // Build a temporary state for the arrival/teleportation message
        AgentWorldState state = AgentWorldState.builder()
                .agentId(event.agentId())
                .agentName(event.agentName())
                .x(event.x())
                .y(event.y())
                .currentLocationId(event.location().getId())
                .status(AgentStatus.IDLE)
                .build();

        log.info("Broadcasting arrival/teleport for agent {} to {} (Location: {})", 
                 event.agentId(), destination, event.location().getName());
        
        messagingTemplate.convertAndSend(destination, state);
    }
}
