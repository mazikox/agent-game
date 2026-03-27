package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.agent.service.AgentService;

import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


/**
 * Listener responsible for detecting if an agent arrived at a portal location
 * and triggering the teleportation via AgentService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortalEventListener {

    private final PortalRepository portalRepository;
    private final AgentService agentService;

    /**
     * Reacts to the AgentArrivedEvent.
     */
    @EventListener
    public void onAgentArrived(AgentArrivedEvent event) {
        log.debug("Checking for portals at destination for agent {}", event.agentId());

        // Check the database for any portal at these coordinates
        portalRepository.findBySourceLocationAndSourceXAndSourceY(event.location(), event.x(), event.y())
                .ifPresent(portal -> {
                    log.info("Portal found! Triggering teleportation for agent {} from {} to {}", 
                             event.agentId(), portal.getSourceLocation().getName(), portal.getTargetLocation().getName());
                    
                    agentService.teleportTo(
                            event.agentId(),
                            portal.getTargetLocation(),
                            portal.getTargetX(),
                            portal.getTargetY()
                    );
                });
    }
}

