package com.agentgierka.mmo.agent.event;

import com.agentgierka.mmo.agent.service.AgentService;

import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * Listener responsible for detecting if an agent arrived at a portal location
 * and triggering the teleportation via AgentService.
 */
@Component
@Order(1)
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
        // Only trigger portals for normal movement to prevent infinite loops and unintended chains
        if (event.type() != MovementType.NORMAL) {
            log.debug("Skipping portal check for agent {} (movement type: {})", event.agentId(), event.type());
            return;
        }

        log.info("Checking for portals for agent {} at {} ({}, {})", event.agentName(), event.location().getName(), event.x(), event.y());

        // Check the database for any portal at these coordinates using Location ID for robustness
        portalRepository.findBySourceLocationIdAndSourceXAndSourceY(event.location().getId(), event.x(), event.y())
                .ifPresentOrElse(portal -> {
                    log.info("Portal found! Triggering teleportation for agent {} to {}", 
                             event.agentName(), portal.getTargetLocation().getName());
                    
                    agentService.teleportTo(
                            event.agentId(),
                            portal.getTargetLocation(),
                            portal.getTargetX(),
                            portal.getTargetY()
                    );
                }, () -> log.debug("No portal found for agent {} at ({}, {})", event.agentId(), event.x(), event.y()));

    }
}

