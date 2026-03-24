package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to final state persistence from Redis to Postgres.
 * Separated to ensure Spring @Transactional proxy works correctly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPersistenceService {

    private final AgentRepository agentRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;

    @Transactional
    public void finalizeMovement(AgentWorldState state) {
        Agent agent = agentRepository.findById(state.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found during movement finalization"));

        agent.setX(state.getX());
        agent.setY(state.getY());
        agent.setStatus(AgentStatus.IDLE);
        agent.setCurrentActionDescription("Arrived at destination (" + state.getX() + ", " + state.getY() + ")");
        
        agentRepository.save(agent);
        agentWorldStateRepository.delete(state.getAgentId());
        
        log.info("Agent {} reached destination and state was flushed to Postgres", agent.getName());
    }
}
