package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.world.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorldStateSynchronizer {

    private final AgentWorldStateRepository agentWorldStateRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void syncMovementAfterCommit(Agent agent) {
        executeAfterCommit(() -> {
            AgentWorldState worldState = AgentWorldState.builder()
                    .agentId(agent.getId())
                    .agentName(agent.getName())
                    .x(agent.getX())
                    .y(agent.getY())
                    .targetX(agent.getTargetX())
                    .targetY(agent.getTargetY())
                    .status(agent.getStatus())
                    .speed(agent.getSpeed())
                    .build();
            agentWorldStateRepository.save(worldState);
        });
    }

    public void clearStateAndPublishArrival(UUID agentId, String agentName, Location location, Integer x, Integer y, MovementType type) {
        executeAfterCommit(() -> {
            agentWorldStateRepository.delete(agentId);
            eventPublisher.publishEvent(new AgentArrivedEvent(agentId, agentName, location, x, y, type));
        });
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
            action.run();
        }
    }
}
