package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that periodically persists the volatile Redis state to PostgreSQL.
 * This prevents massive progress loss if Redis or the application fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentWorldStateCheckpointService {

    private final AgentWorldStateRepository agentWorldStateRepository;
    private final AgentRepository agentRepository;

    /**
     * Periodically saves current coordinates of moving agents to the persistent database.
     * Running every 10 seconds is a good balance between data safety and DB load.
     */
    @Scheduled(fixedRateString = "${game.engine.checkpoint-rate:10000}")
    @Transactional
    public void performCheckpoint() {
        List<AgentWorldState> activeAgents = agentWorldStateRepository.findAllActive();
        
        if (activeAgents.isEmpty()) {
            return;
        }

        log.debug("Performing position checkpoint for {} moving agents...", activeAgents.size());
        
        int updatedCount = 0;
        for (AgentWorldState state : activeAgents) {
            try {
                agentRepository.updatePosition(state.getAgentId(), state.getX(), state.getY());
                updatedCount++;
            } catch (Exception e) {
                log.error("Failed to checkpoint position for agent {}", state.getAgentId(), e);
            }
        }

        if (updatedCount > 0) {
            log.info("Checkpoint finished. Persisted positions for {} agents.", updatedCount);
        }
    }
}
