package com.agentgierka.mmo.ai.service;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentThinkingService {

    private final AgentRepository agentRepository;
    private final PortalRepository portalRepository;
    private final WorldStateSynchronizer worldStateSynchronizer;
    private final Brain brain;

    @Transactional
    public void processThinking(UUID agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));


        log.info("Agent {} is thinking about goal: '{}'", agent.getName(), agent.getGoal());

        List<String> portals = portalRepository.findAllBySourceLocationId(agent.getCurrentLocation().getId())
                .stream()
                .map(p -> String.format("Portal to %s at (%d, %d)",
                        p.getTargetLocation().getName(), p.getSourceX(), p.getSourceY()))
                .collect(Collectors.toList());

        log.info("--- AI THINKING START for Agent: {} ---", agent.getName());
        log.info("Perception: Goal='{}', Location='{}', Coordinates=({},{}), Portals={}",
                 agent.getGoal(), agent.getCurrentLocation().getName(), agent.getX(), agent.getY(), portals);

        Thought thought = brain.think(agent.preparePerception(portals));

        log.info("AI DECISION for {}: Action='{}', Next Goal='{}', MoveTo=({}, {}), Status={}",
                 agent.getName(), thought.actionSummary(), thought.nextGoal(), thought.targetX(), thought.targetY(), thought.status());
        log.info("--- AI THINKING END ---");

        agent.applyThought(thought);

        agentRepository.save(agent);

        if (agent.getStatus() == AgentStatus.MOVING) {
            worldStateSynchronizer.syncMovementAfterCommit(agent);
        }
    }
}
