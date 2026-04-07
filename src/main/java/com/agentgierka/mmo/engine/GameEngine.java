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
    private final EngineControl engineControl;

    /**
     * Primary tick processing loop using Redis World State for high-performance updates.
     */
    @Scheduled(fixedRateString = "${game.engine.tick-rate:1000}")
    public void tick() {
        if (!engineControl.isReady()) {
            return;
        }
        processMovement();
    }

    private void processMovement() {
        List<AgentWorldState> activeAgents = agentWorldStateRepository.findAllActive();

        if (activeAgents.isEmpty()) {
            return;
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (AgentWorldState state : activeAgents) {
                executor.submit(() -> {
                    if (state.updatePosition()) {
                        String name = state.getAgentName() != null ? state.getAgentName() : state.getAgentId().toString();
                        log.info("Agent {} successfully reached target at ({}, {})", name, state.getX(), state.getY());
                        agentPersistenceService.finalizeMovement(state);
                    } else {
                        agentWorldStateRepository.updateAtomic(state);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error during parallel movement processing", e);
        }
    }

}
