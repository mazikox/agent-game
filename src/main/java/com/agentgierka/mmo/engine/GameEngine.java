package com.agentgierka.mmo.engine;

import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The core game engine responsible for processing the world state at regular intervals.
 * It manages agent movements, state transitions, and asynchronous events.
 * 
 * TODO: Consider Scoped Values (Java 25) to pass the current "tick" context 
 * (timestamp, serverId) deep into agent logic, eliminating verbose parameter passing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameEngine {
 
    private final AgentWorldStateRepository agentWorldStateRepository;
    private final AgentPersistenceService agentPersistenceService;

    /**
     * Primary tick processing loop using Redis World State for high-performance updates.
     */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        processMovement();
    }

    private void processMovement() {
        List<AgentWorldState> activeAgents = agentWorldStateRepository.findAllActive();

        if (activeAgents.isEmpty()) {
            return;
        }
 
        // TODO: Use Java 25 Structured Concurrency (StructuredTaskScope) for parallel 
        // movement processing to scale to thousands of agents without blocking the main tick thread.
        for (AgentWorldState state : activeAgents) {
            updateAgentPosition(state);
        }
    }

    private void updateAgentPosition(AgentWorldState state) {
        Integer curX = state.getX();
        Integer curY = state.getY();
        Integer tarX = state.getTargetX();
        Integer tarY = state.getTargetY();

        // 1. Calculate next step
        if (!curX.equals(tarX)) {
            state.setX(curX < tarX ? curX + 1 : curX - 1);
        }
        if (!curY.equals(tarY)) {
            state.setY(curY < tarY ? curY + 1 : curY - 1);
        }

        // 2. Check if destination is reached
        if (state.getX().equals(tarX) && state.getY().equals(tarY)) {
            agentPersistenceService.finalizeMovement(state);
        } else {
            // Update Redis state for the next tick
            agentWorldStateRepository.save(state);
        }
    }

}
