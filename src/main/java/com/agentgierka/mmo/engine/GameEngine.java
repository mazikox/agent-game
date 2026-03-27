package com.agentgierka.mmo.engine;

import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;

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
 
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (AgentWorldState state : activeAgents) {
                executor.submit(() -> updateAgentPosition(state));
            }
        } catch (Exception e) {
            log.error("Error during parallel movement processing", e);
        }
    }

    private void updateAgentPosition(AgentWorldState state) {
        Integer curX = state.getX();
        Integer curY = state.getY();
        Integer tarX = state.getTargetX();
        Integer tarY = state.getTargetY();

        // 1. Calculate next step
        int speed = state.getSpeed() != null ? state.getSpeed() : 1;

        if (!curX.equals(tarX)) {
            int diff = tarX - curX;
            int step = Math.min(Math.abs(diff), speed);
            state.setX(curX + (diff > 0 ? step : -step));
        }
        if (!curY.equals(tarY)) {
            int diff = tarY - curY;
            int step = Math.min(Math.abs(diff), speed);
            state.setY(curY + (diff > 0 ? step : -step));
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
