package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
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

    @Transactional
    public void finalizeMovement(AgentWorldState state) {
        Agent agent = agentRepository.findById(state.getAgentId())
                .orElseThrow(() -> new AgentNotFoundException(state.getAgentId().toString()));


        agent.completeMovement(state.getX(), state.getY());
        
        agentRepository.save(agent);
        agentWorldStateRepository.delete(state.getAgentId());
        
        eventPublisher.publishEvent(new AgentArrivedEvent(
                agent.getId(),
                agent.getCurrentLocation(),
                agent.getX(),
                agent.getY(),
                MovementType.NORMAL
        ));
        
        log.info("Agent {} reached destination and event was published", agent.getName());
    }
}
