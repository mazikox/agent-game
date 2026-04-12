package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.event.AgentGoalCompletedEvent;
import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.world.service.WorldInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPersistenceService {

    private final AgentRepository agentRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WorldInteractionService worldInteractionService;
    private final AgentService agentService;

    @Transactional
    public void finalizeMovement(AgentWorldState state) {
        Agent agent = agentRepository.findById(state.getAgentId())
                .orElseThrow(() -> new AgentNotFoundException(state.getAgentId().toString()));

        agent.completeMovement(state.getX(), state.getY());
        
        agentRepository.save(agent);
        agentWorldStateRepository.delete(state.getAgentId());
        
        // Trigger-Aware logic: check if the destination has a portal or other interaction
        var portal = worldInteractionService.findPortalAt(
                agent.getCurrentLocation().getId(), agent.getX(), agent.getY());

        if (portal.isPresent()) {
            var p = portal.get();
            log.info("Agent {} stepped on a portal. Triggering teleportation to {}...", 
                     agent.getName(), p.getTargetLocation().getName());
            
            agentService.teleportTo(agent.getId(), p.getTargetLocation(), p.getTargetX(), p.getTargetY());
        } else {
            // No trigger found, publish the final goal completion event
            eventPublisher.publishEvent(new AgentGoalCompletedEvent(
                    agent.getId(),
                    agent.getName(),
                    agent.getCurrentLocation(),
                    agent.getX(),
                    agent.getY()
            ));
            log.info("Agent {} reached destination and mission was completed", agent.getName());
        }
    }
}
