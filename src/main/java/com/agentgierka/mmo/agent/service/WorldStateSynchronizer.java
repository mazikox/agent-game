package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.event.*;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorldStateSynchronizer {

    private final AgentWorldStateRepository agentWorldStateRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CreatureInstanceRepository creatureInstanceRepository;
    private final ConcurrentHashMap<UUID, AtomicLong> versionTracker = new ConcurrentHashMap<>();

    /**
     * Saves agent's current state to Redis without publishing any WebSocket event.
     * Call this when you need Redis sync but the WebSocket event is published separately.
     */
    public void syncToRedis(Agent agent) {
        executeAfterCommit(() -> {
            // Centralized version safety: delete the old key so the version starts fresh at 0
            agentWorldStateRepository.delete(agent.getId());

            AgentWorldState worldState = AgentWorldState.fromAgent(agent);
            if (agent.getTargetId() != null) {
                CreatureInstance target = creatureInstanceRepository.findById(agent.getTargetId());
                if (target != null) {
                    worldState = worldState.toBuilder()
                        .targetName(target.getName())
                        .targetHp(target.getCurrentHp())
                        .targetMaxHp(target.getMaxHp())
                        .build();
                }
            }
            agentWorldStateRepository.save(worldState);
            log.debug("Redis state synced for agent {}", agent.getId());
        });
    }

    /**
     * Publishes a movement tick event containing only spatial parameters.
     */
    public void publishMovedEvent(Agent agent) {
        executeAfterCommit(() -> {
            long version = incrementAndGetVersion(agent.getId());
            eventPublisher.publishEvent(new AgentMovedEvent(
                agent.getId(),
                agent.getName(),
                agent.getCurrentLocation().getId(),
                agent.getX(),
                agent.getY(),
                version
            ));
        });
    }

    /**
     * Publishes a status changed event when the agent transitions between behaviors.
     */
    public void publishStatusChangedEvent(Agent agent) {
        executeAfterCommit(() -> {
            long version = incrementAndGetVersion(agent.getId());
            eventPublisher.publishEvent(new AgentStatusChangedEvent(
                agent.getId(),
                agent.getStatus(),
                agent.getCurrentActionDescription(),
                version
            ));

            // Required for frontend interpolation — prevents visual drift during abrupt agent stops (e.g. entering combat or manual stop).
            // We only publish this spatial synchronization if the agent is NOT starting a movement (avoiding version conflicts with the tick stream).
            if (agent.getStatus() != AgentStatus.MOVING) {
                eventPublisher.publishEvent(new AgentMovedEvent(
                    agent.getId(),
                    agent.getName(),
                    agent.getCurrentLocation() != null ? agent.getCurrentLocation().getId() : null,
                    agent.getX(),
                    agent.getY(),
                    version
                ));
            }
        });
    }

    /**
     * Publishes a health changed event when the agent takes damage or heals.
     */
    public void publishHealthChangedEvent(Agent agent) {
        executeAfterCommit(() -> {
            long version = incrementAndGetVersion(agent.getId());
            eventPublisher.publishEvent(new AgentHealthChangedEvent(
                agent.getId(),
                agent.getStats().getHp(),
                agent.getStats().getMaxHp(),
                version
            ));
        });
    }

    /**
     * Publishes a combat target changed event.
     */
    public void publishCombatTargetChangedEvent(Agent agent, CreatureInstance target) {
        executeAfterCommit(() -> {
            long version = incrementAndGetVersion(agent.getId());
            eventPublisher.publishEvent(new AgentCombatTargetChangedEvent(
                agent.getId(),
                target != null ? target.getInstanceId() : null,
                target != null ? target.getName() : null,
                target != null ? target.getCurrentHp() : 0,
                target != null ? target.getMaxHp() : 0,
                version
            ));
        });
    }

    public void clearStateAndPublishArrival(UUID agentId, String agentName, Location location, Integer x, Integer y, MovementType type) {
        executeAfterCommit(() -> {
            agentWorldStateRepository.delete(agentId);
            eventPublisher.publishEvent(new AgentArrivedAtWaypointEvent(agentId, agentName, location, x, y, type));
        });
    }

    private long incrementAndGetVersion(UUID agentId) {
        return versionTracker.computeIfAbsent(agentId, id -> new AtomicLong(0)).incrementAndGet();
    }

    private void executeAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            log.warn("No active transaction — executing synchronously. This may cause state inconsistency.");
            action.run();
        }
    }
}
