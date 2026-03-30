package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.event.AgentArrivedEvent;
import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.exception.InvalidMovementException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.agentgierka.mmo.ai.service.AgentThinkingService agentThinkingService;

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    @Transactional
    public void assignGoal(UUID agentId, String goal) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        agent.assignGoal(goal);
        agentRepository.save(agent);
        
        agentThinkingService.processThinking(agentId);
    }

    @Transactional(readOnly = true)
    public Agent findById(UUID id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new AgentNotFoundException(id.toString()));

        AgentWorldState worldState = agentWorldStateRepository.findById(id);
        if (worldState != null) {
            agent.syncWithWorldState(worldState);
        }

        return agent;
    }

    @Transactional
    public Agent moveTo(UUID agentId, Integer targetX, Integer targetY) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        if (agent.getCurrentLocation() != null) {
            validateBounds(agent, targetX, targetY);
        }

        agent.startMovement(targetX, targetY, "Preparing to move to (" + targetX + ", " + targetY + ")");
        Agent savedAgent = agentRepository.save(agent);

        // Synchronize with Redis only after the database transaction is committed to ensure consistency.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    AgentWorldState worldState = AgentWorldState.builder()
                            .agentId(agentId)
                            .x(agent.getX())
                            .y(agent.getY())
                            .targetX(targetX)
                            .targetY(targetY)
                            .status(AgentStatus.MOVING)
                            .speed(agent.getSpeed())
                            .build();
                    agentWorldStateRepository.save(worldState);
                }
            });
        }

        return savedAgent;
    }

    private void validateBounds(Agent agent, Integer x, Integer y) {
        if (x < 0 || x > agent.getCurrentLocation().getWidth() ||
            y < 0 || y > agent.getCurrentLocation().getHeight()) {
            throw new InvalidMovementException("Target coordinates (" + x + "," + y + ") are outside the location boundaries.");
        }
    }

    @Transactional
    public Agent teleportTo(UUID agentId, com.agentgierka.mmo.world.Location location, Integer x, Integer y) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        agent.teleport(location, x, y);

        Agent savedAgent = agentRepository.save(agent);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    agentWorldStateRepository.delete(agentId);

                    eventPublisher.publishEvent(new AgentArrivedEvent(
                            agentId,
                            location,
                            x,
                            y,
                            MovementType.TELEPORT
                    ));
                }
            });
        }
        return savedAgent;
    }

    @Transactional
    public Agent updateStatus(UUID agentId, AgentStatus status, String description) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        agent.updateStatus(status, description);

        return agentRepository.save(agent);
    }
}
