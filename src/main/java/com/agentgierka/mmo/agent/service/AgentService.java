package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.exception.InvalidMovementException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Service responsible for managing agent state and activities.
 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;

    /**
     * Lists all agents in the world.
     */
    public java.util.List<Agent> findAll() {
        return agentRepository.findAll();
    }

    /**
     * Finds an agent by ID with Hybrid Read logic.
     * Returns the most up-to-date state (from Redis if moving, or Postgres if idle).
     */
    @Transactional(readOnly = true)
    public Agent findById(UUID id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new AgentNotFoundException(id.toString()));

        // Check if agent is currently moving (recorded in Redis)
        AgentWorldState worldState = agentWorldStateRepository.findById(id);
        if (worldState != null) {
            // Override with real-time world state from Redis
            agent.setX(worldState.getX());
            agent.setY(worldState.getY());
            agent.setStatus(worldState.getStatus());
        }

        return agent;
    }

    /**
     * Sets a new movement target for the agent using a Redis-first intent model.
     * Persistence is maintained in Postgres, while real-time state moves to Redis.
     */
    @Transactional
    public Agent moveTo(UUID agentId, Integer targetX, Integer targetY) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        if (agent.getCurrentLocation() != null) {
            validateBounds(agent, targetX, targetY);
        }

        // 1. Update Persistent State (Postgres)
        agent.setTargetX(targetX);
        agent.setTargetY(targetY);
        agent.setStatus(AgentStatus.MOVING);
        agent.setCurrentActionDescription("Preparing to move to (" + targetX + ", " + targetY + ")");
        Agent savedAgent = agentRepository.save(agent);

        // 2. Push to World State (Redis) ONLY after Postgres transaction is committed
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

    /**
     * Teleports an agent to a specific location and coordinates.
     * This is a forced movement that bypasses regular travel.
     */
    @Transactional
    public Agent teleportTo(UUID agentId, com.agentgierka.mmo.world.Location location, Integer x, Integer y) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        agent.setCurrentLocation(location);
        agent.setX(x);
        agent.setY(y);
        agent.setStatus(AgentStatus.IDLE);
        agent.setCurrentActionDescription("Teleported to " + location.getName());

        Agent savedAgent = agentRepository.save(agent);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    agentWorldStateRepository.delete(agentId); // Ensure Redis state is cleared correctly
                }
            });
        }
        return savedAgent;
    }

    /**
     * Updates the agent's status and narrative description.
     */
    @Transactional
    public Agent updateStatus(UUID agentId, AgentStatus status, String description) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        agent.setStatus(status);
        agent.setCurrentActionDescription(description);

        return agentRepository.save(agent);
    }
}
