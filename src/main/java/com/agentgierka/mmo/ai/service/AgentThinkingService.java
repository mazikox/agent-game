package com.agentgierka.mmo.ai.service;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeExecutor;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeRegistry;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.agentgierka.mmo.ai.port.GoalPlanner;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.model.CreatureState;
import com.agentgierka.mmo.agent.event.AgentConsoleLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentThinkingService {

    private final AgentRepository agentRepository;
    private final PortalRepository portalRepository;
    private final CreatureInstanceRepository creatureInstanceRepository;
    private final WorldStateSynchronizer worldStateSynchronizer;
    private final ApplicationEventPublisher eventPublisher;
    private final GoalPlanner goalPlanner;
    private final BehaviorTreeRegistry behaviorTreeRegistry;
    private final BehaviorTreeExecutor behaviorTreeExecutor;

    @Transactional
    public void processThinking(UUID agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        if (agent.getCurrentLocation() == null) {
            log.warn("Agent {} has no location assigned. Skipping thinking process.", agent.getName());
            return;
        }

        if (!agent.hasActiveGoal()) {
            return;
        }

        List<String> portals = portalRepository.findAllBySourceLocationId(agent.getCurrentLocation().getId())
                .stream()
                .map(p -> String.format("Portal to %s at (%d, %d)",
                        p.getTargetLocation().getName(), p.getSourceX(), p.getSourceY()))
                .collect(Collectors.toList());

        List<String> creatures = creatureInstanceRepository.findAllByLocationId(agent.getCurrentLocation().getId())
                .stream()
                .filter(c -> c.getState() != CreatureState.DEAD)
                .map(c -> String.format("%s at (%d, %d) HP:%d/%d [State: %s]",
                        c.getName(), c.getX(), c.getY(), c.getCurrentHp(), c.getMaxHp(), c.getState()))
                .collect(Collectors.toList());

        try {
            if (behaviorTreeRegistry.get(agentId).isEmpty()) {
                log.info("Agent {} is planning goal: '{}'", agent.getName(), agent.getGoal());
                Optional<BehaviorNode> plannedTree = goalPlanner.planGoal(agent.getGoal(), agent.preparePerception(portals, creatures));
                
                if (plannedTree.isPresent()) {
                    behaviorTreeRegistry.register(agentId, plannedTree.get());
                    String planMsg = String.format("[AI] Created new Behavior Tree plan for goal: '%s'", agent.getGoal());
                    eventPublisher.publishEvent(new AgentConsoleLogEvent(agent.getId(), planMsg));
                } else {
                    log.warn("Failed to plan tree for agent {}", agent.getName());
                    agent.updateStatus(AgentStatus.IDLE, "AI planner failed to create a plan.");
                    agentRepository.save(agent);
                    return;
                }
            }

            behaviorTreeExecutor.tick(agent);
            ensureFollowUpTickIfIdle(agent);

            agentRepository.save(agent);
            worldStateSynchronizer.syncMovementAfterCommit(agent);

        } catch (Exception e) {
            handleThinkingError(agent, e);
        }
    }

    private void ensureFollowUpTickIfIdle(Agent agent) {
        if (shouldFireFollowUpTick(agent)) {
            log.info("Agent {} is IDLE with active goal after tick — firing immediate follow-up tick.", agent.getName());
            behaviorTreeExecutor.tick(agent);
        }
    }

    private boolean shouldFireFollowUpTick(Agent agent) {
        return agent.getStatus() == AgentStatus.IDLE
                && agent.hasActiveGoal()
                && behaviorTreeRegistry.get(agent.getId()).isPresent();
    }

    private void handleThinkingError(Agent agent, Exception e) {
        log.error("Error during AI thinking process for agent {}: {}", agent.getName(), e.getMessage(), e);
        String errorMsg = Optional.ofNullable(e.getMessage())
                .map(msg -> msg.length() > 200 ? msg.substring(0, 197) + "..." : msg)
                .orElse("Unknown error");
        
        agent.updateStatus(AgentStatus.IDLE, "AI thinking error: " + errorMsg);
        agentRepository.save(agent);
    }
}
