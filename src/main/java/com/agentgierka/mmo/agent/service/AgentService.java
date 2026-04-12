package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.event.GoalAssignedEvent;
import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.exception.InvalidMovementException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.agent.model.MovementType;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;
    private final WorldStateSynchronizer worldStateSynchronizer;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    @Transactional
    public void assignGoal(UUID agentId, String goal) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        // Initialize quota from player's tier/maxThinkingSteps, fallback to 1
        int initialQuota = (agent.getOwner() != null && agent.getOwner().getMaxThinkingSteps() != null) 
                ? agent.getOwner().getMaxThinkingSteps() : 1;
        
        agent.assignGoal(goal, initialQuota);
        agentRepository.save(agent);

        worldStateSynchronizer.syncMovementAfterCommit(agent);

        eventPublisher.publishEvent(new GoalAssignedEvent(agentId));
    }

    @Transactional
    public void interruptAgent(UUID agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        agent.cancelCurrentGoal();
        agentRepository.save(agent);
        
        // Sync with Redis to stop movement IMMEDIATELY
        worldStateSynchronizer.syncMovementAfterCommit(agent);
    }

    @Transactional(readOnly = true)
    public Agent findById(UUID id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new AgentNotFoundException(id.toString()));

        entityManager.detach(agent);

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

        worldStateSynchronizer.syncMovementAfterCommit(agent);

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

        // Cooldown check: prevent teleporting more than once every 5 seconds
        if (agent.getLastTeleportAt() != null && 
            agent.getLastTeleportAt().plusSeconds(5).isAfter(LocalDateTime.now())) {
            log.warn("Teleportation blocked by cooldown for agent: {}", agent.getName());
            return agent; 
        }

        agent.teleport(location, x, y);

        Agent savedAgent = agentRepository.save(agent);

        worldStateSynchronizer.clearStateAndPublishArrival(agentId, agent.getName(), location, x, y, MovementType.TELEPORT);

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
