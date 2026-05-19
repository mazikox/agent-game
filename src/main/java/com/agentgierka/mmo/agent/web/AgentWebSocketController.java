package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.event.*;
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
    public void onAgentMoved(AgentMovedEvent event) {
        String destination = "/topic/agents/" + event.agentId() + "/position";
        log.debug("Broadcasting position update for agent {} to {}", event.agentId(), destination);
        messagingTemplate.convertAndSend(
            destination,
            new AgentPositionDto(event.x(), event.y(), event.locationId(), event.version())
        );
    }

    @EventListener
    public void onAgentStatusChanged(AgentStatusChangedEvent event) {
        String destination = "/topic/agents/" + event.agentId() + "/status";
        log.debug("Broadcasting status update for agent {} to {}", event.agentId(), destination);
        messagingTemplate.convertAndSend(
            destination,
            new AgentStatusDto(event.status(), event.actionDescription(), event.version())
        );
    }

    @EventListener
    public void onAgentHealthChanged(AgentHealthChangedEvent event) {
        String destination = "/topic/agents/" + event.agentId() + "/health";
        log.debug("Broadcasting health update for agent {} to {}", event.agentId(), destination);
        messagingTemplate.convertAndSend(
            destination,
            new AgentHealthDto(event.hp(), event.maxHp(), event.version())
        );
    }

    @EventListener
    public void onCombatTargetChanged(AgentCombatTargetChangedEvent event) {
        String destination = "/topic/agents/" + event.agentId() + "/target";
        log.debug("Broadcasting target update for agent {} to {}", event.agentId(), destination);
        messagingTemplate.convertAndSend(
            destination,
            new AgentTargetDto(
                event.targetId(),
                event.targetName(),
                event.targetHp(),
                event.targetMaxHp(),
                event.version()
            )
        );
    }

    @EventListener
    public void onCombatLog(com.agentgierka.mmo.combat.event.CombatLogEvent event) {
        String destination = "/topic/agents/" + event.agentId() + "/logs";
        log.debug("Broadcasting combat log to {}: {}", destination, event.message());
        messagingTemplate.convertAndSend(destination, event.message());
    }

    @EventListener
    public void onAgentConsoleLog(AgentConsoleLogEvent event) {
        String destination = "/topic/agents/" + event.agentId() + "/logs";
        log.debug("Broadcasting agent console log to {}: {}", destination, event.message());
        messagingTemplate.convertAndSend(destination, event.message());
    }
}
