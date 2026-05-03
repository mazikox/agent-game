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
import com.agentgierka.mmo.ai.event.AiThinkingRequiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgressRegistry;

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
    private final GoalProgressRegistry goalProgressRegistry;

    @Transactional
    public void processThinking(UUID agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        if (agent.getCurrentLocation() == null) {
            log.warn("Agent {} has no location assigned. Skipping thinking process.", agent.getName());
            return;
        }

        if (!agent.hasActiveGoal()) {
            abortCurrentPlan(agentId);
            return;
        }

        try {
            if (behaviorTreeRegistry.get(agentId).isEmpty()) {
                List<String> portals = fetchPortals(agent);
                List<String> creatures = fetchCreatures(agent);
                planNewGoal(agent, portals, creatures);
            }

            NodeStatus status = behaviorTreeExecutor.tick(agent);
            
            if (status == NodeStatus.SUCCESS || status == NodeStatus.FAILURE) {
                handleGoalCompletion(agent, status);
            } else {
                ensureFollowUpTickIfIdle(agent);
            }

            agentRepository.save(agent);
            worldStateSynchronizer.syncMovementAfterCommit(agent);

        } catch (Exception e) {
            handleThinkingError(agent, e);
        }
    }

    public void abortCurrentPlan(UUID agentId) {
        log.info("Aborting current plan and clearing progress for agent {}", agentId);
        behaviorTreeRegistry.remove(agentId);
        goalProgressRegistry.remove(agentId);
    }

    private void handleGoalCompletion(Agent agent, NodeStatus status) {
        log.info("Goal '{}' finished with status {} for agent {}. Cleaning up...", 
                 agent.getGoal(), status, agent.getName());
        abortCurrentPlan(agent.getId());
        agent.clearGoal();
    }

    private List<String> fetchPortals(Agent agent) {
        return portalRepository.findAllBySourceLocationId(agent.getCurrentLocation().getId())
                .stream()
                .map(p -> String.format("Portal to %s at (%d, %d)",
                        p.getTargetLocation().getName(), p.getSourceX(), p.getSourceY()))
                .collect(Collectors.toList());
    }

    private List<String> fetchCreatures(Agent agent) {
        return creatureInstanceRepository.findAllByLocationId(agent.getCurrentLocation().getId())
                .stream()
                .filter(c -> c.getState() != CreatureState.DEAD)
                .map(c -> String.format("%s at (%d, %d) HP:%d/%d [State: %s]",
                        c.getName(), c.getX(), c.getY(), c.getCurrentHp(), c.getMaxHp(), c.getState()))
                .collect(Collectors.toList());
    }

    private void planNewGoal(Agent agent, List<String> portals, List<String> creatures) {
        log.info("Agent {} is planning goal: '{}'", agent.getName(), agent.getGoal());
        Optional<BehaviorNode> plannedTree = goalPlanner.planGoal(agent.getGoal(), agent.preparePerception(portals, creatures));
        
        if (plannedTree.isPresent()) {
            BehaviorNode node = plannedTree.get();
            behaviorTreeRegistry.register(agent.getId(), node);
            agent.logAiDecision(node.describe());
            agentRepository.save(agent);
            
            String planMsg = String.format("[AI] Created new Behavior Tree plan for goal: '%s'", agent.getGoal());
            eventPublisher.publishEvent(new AgentConsoleLogEvent(agent.getId(), planMsg));
        } else {
            log.warn("Failed to plan tree for agent {}", agent.getName());
            agent.updateStatus(AgentStatus.IDLE, "AI planner failed to create a plan.");
        }
    }

    private void ensureFollowUpTickIfIdle(Agent agent) {
        if (shouldFireFollowUpTick(agent)) {
            log.debug("Agent {} is IDLE with active goal after tick — scheduling follow-up tick via event.", agent.getName());
            eventPublisher.publishEvent(new AiThinkingRequiredEvent(agent.getId()));
        }
    }

    private boolean shouldFireFollowUpTick(Agent agent) {
        return agent.getStatus() == AgentStatus.IDLE
                && agent.hasActiveGoal()
                && behaviorTreeRegistry.get(agent.getId()).isPresent();
    }

    private void handleThinkingError(Agent agent, Exception e) {
        log.error("AI thinking error for agent {}: {}", agent.getName(), e.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("Stack trace for agent {}:", agent.getName(), e);
        }

        String errorMsg = Optional.ofNullable(e.getMessage())
                .map(msg -> msg.length() > 200 ? msg.substring(0, 197) + "..." : msg)
                .orElse("Unknown error");
        
        agent.updateStatus(AgentStatus.IDLE, "AI thinking error: " + errorMsg);
        abortCurrentPlan(agent.getId());
        agentRepository.save(agent);
    }
}
