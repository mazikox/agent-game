package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for recovering agent movement state from Postgres to Redis
 * during application startup. This ensures that a server restart does not
 * cause agents to get stuck.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentWorldStateRecoveryService {

    private final AgentRepository agentRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverStatesOnStartup() {
        log.info("Starting Agent World State recovery process...");
        
        List<Agent> movingAgents = agentRepository.findByStatus(AgentStatus.MOVING);
        
        if (movingAgents.isEmpty()) {
            log.info("No agents found in MOVING status. Recovery skipped.");
            return;
        }

        int recoveredCount = 0;
        for (Agent agent : movingAgents) {
            AgentWorldState existing = agentWorldStateRepository.findById(agent.getId());
            if (existing == null) {
                log.info("Recovering movement state for agent: {} ({})", agent.getName(), agent.getId());
                AgentWorldState recoveredState = AgentWorldState.fromAgent(agent);
                agentWorldStateRepository.save(recoveredState);
                recoveredCount++;
            }
        }

        log.info("Recovery finished. Processed {} agents, restored {} states to Redis.", 
                 movingAgents.size(), recoveredCount);
    }
}
