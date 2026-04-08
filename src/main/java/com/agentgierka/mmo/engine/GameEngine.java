package com.agentgierka.mmo.engine;

import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.agent.service.AgentPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The core game engine responsible for processing the world state at regular intervals.
 * It manages agent movements, state transitions, and asynchronous events.
 */
@Service
@Slf4j
public class GameEngine {

    private final AgentWorldStateRepository agentWorldStateRepository;
    private final AgentPersistenceService agentPersistenceService;
    private final EngineControl engineControl;
    private final AsyncTaskExecutor taskExecutor;
    private final Duration tickTimeout;

    public GameEngine(AgentWorldStateRepository agentWorldStateRepository,
                      AgentPersistenceService agentPersistenceService,
                      EngineControl engineControl,
                      @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor,
                      @Value("${game.engine.tick-timeout:5s}") Duration tickTimeout) {
        this.agentWorldStateRepository = agentWorldStateRepository;
        this.agentPersistenceService = agentPersistenceService;
        this.engineControl = engineControl;
        this.taskExecutor = taskExecutor;
        this.tickTimeout = tickTimeout;
    }

    /**
     * Primary tick processing loop using Redis World State for high-performance updates.
     */
    @Scheduled(fixedRateString = "${game.engine.tick-rate:1s}")
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

        List<CompletableFuture<Void>> futures = activeAgents.stream()
                .map(state -> CompletableFuture.runAsync(() -> processAgent(state), taskExecutor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(tickTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Game engine tick timed out after {}. Some updates may be postponed to the next tick.", tickTimeout);
        } catch (Exception e) {
            log.error("Critical error during parallel movement processing", e);
        }
    }

    private void processAgent(AgentWorldState state) {
        try {
            if (state.updatePosition()) {
                String name = state.getAgentName() != null ? state.getAgentName() : state.getAgentId().toString();
                log.info("Agent {} successfully reached target at ({}, {})", name, state.getX(), state.getY());
                agentPersistenceService.finalizeMovement(state);
            } else {
                agentWorldStateRepository.updateAtomic(state);
            }
        } catch (Exception e) {
            log.error("Failed to process agent {}", state.getAgentId(), e);
        }
    }
}
