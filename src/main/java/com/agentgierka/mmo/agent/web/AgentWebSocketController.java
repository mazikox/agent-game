package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.event.AgentGoalCompletedEvent;
import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
import com.agentgierka.mmo.agent.event.AgentStateUpdatedEvent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.model.MovementType;
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
    public void onAgentGoalCompleted(AgentGoalCompletedEvent event) {
        broadcast(event.agentId(), event.agentName(), event.location().getId(), event.x(), event.y());
    }

    @EventListener
    public void onAgentArrived(AgentArrivedEvent event) {
        // We only care about TELEPORT arrivals here, as NORMAL arrivals 
        // are now handled via AgentGoalCompletedEvent (if they aren't portals)
        if (event.type() == MovementType.TELEPORT) {
            broadcast(event.agentId(), event.agentName(), event.location().getId(), event.x(), event.y());
        }
    }

    private void broadcast(java.util.UUID agentId, String agentName, java.util.UUID locationId, int x, int y) {
        String destination = "/topic/agents/" + agentId;
        
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .agentName(agentName)
                .x(x)
                .y(y)
                .currentLocationId(locationId)
                .status(AgentStatus.IDLE)
                .build();

        log.info("Broadcasting final state for agent {} to {} (Location ID: {})", 
                 agentId, destination, locationId);
        
        messagingTemplate.convertAndSend(destination, state);
    }
}
