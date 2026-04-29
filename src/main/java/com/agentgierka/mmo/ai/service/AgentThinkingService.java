package com.agentgierka.mmo.ai.service;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.ActionStep;
import com.agentgierka.mmo.agent.model.GoalExecutionMode;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeExecutor;
import com.agentgierka.mmo.ai.behaviortree.BehaviorTreeRegistry;
import com.agentgierka.mmo.ai.model.Decision;
import com.agentgierka.mmo.ai.model.ActionType;
import com.agentgierka.mmo.ai.model.QualifierType;
import com.agentgierka.mmo.ai.model.Direction;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import com.agentgierka.mmo.agent.service.ActionResolverService;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.ai.port.Brain;
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
    private final Brain brain;
    private final ApplicationEventPublisher eventPublisher;
    private final ActionResolverService actionResolverService;
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

        if (agent.getExecutionMode() == GoalExecutionMode.BEHAVIOR_TREE) {
            behaviorTreeExecutor.tick(agent);
            agentRepository.save(agent);
            worldStateSynchronizer.syncMovementAfterCommit(agent);
            return;
        }

        log.info("Agent {} is thinking about goal: '{}'", agent.getName(), agent.getGoal());

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

        if (agent.hasActiveGoal() && agent.getExecutionMode() == GoalExecutionMode.SIMPLE) {
            Optional<BehaviorNode> plannedTree = goalPlanner.planGoal(agent.getGoal(), agent.preparePerception(portals, creatures));
            if (plannedTree.isPresent()) {
                log.info("Agent {} switching to BEHAVIOR_TREE mode based on goal complexity.", agent.getName());
                agent.changeExecutionMode(GoalExecutionMode.BEHAVIOR_TREE);
                behaviorTreeRegistry.register(agent.getId(), plannedTree.get());
                
                behaviorTreeExecutor.tick(agent);
                agentRepository.save(agent);
                worldStateSynchronizer.syncMovementAfterCommit(agent);
                return;
            }
        }

        try {
            log.info("--- AI THINKING START for Agent: {} ---", agent.getName());
            Thought thought = brain.think(agent.preparePerception(portals, creatures));
            log.info("--- AI THINKING END ---");

            if (thought != null && thought.actions() != null && !thought.actions().isEmpty()) {
                List<ActionStep> steps = thought.actions().stream()
                        .map(d -> ActionStep.create(
                                safeParseEnum(d.actionType(), ActionType.class, ActionType.UNKNOWN),
                                d.targetIndex(),
                                safeParseEnum(d.qualifier(), QualifierType.class, null),
                                d.rawX(),
                                d.rawY(),
                                safeParseEnum(d.direction(), Direction.class, null),
                                d.steps(),
                                d.actionSummary()
                        ))
                        .collect(Collectors.toList());

                log.info("AI plan for {}: {}", agent.getName(), steps.stream()
                        .map(s -> s.getActionType() + "[" + 
                                 (s.getDirection() != null ? "dir=" + s.getDirection() + ",steps=" + s.getSteps() : "") +
                                 (s.getRawX() != null ? "X=" + s.getRawX() + ",Y=" + s.getRawY() : "") + "] (" +
                                 s.getActionSummary() + ")")
                        .collect(Collectors.joining(" -> ")));

                agent.enqueueActions(steps);

                String sequenceSummary = thought.actions().stream()
                        .map(Decision::actionSummary)
                        .collect(Collectors.joining(" -> "));

                // Domain persistence fallback (inline for now to keep Agent clean)
                String logMessage = "[AKCJA] AI: " + sequenceSummary;
                if (logMessage.length() > 255) {
                    logMessage = logMessage.substring(0, 252) + "...";
                }
                agent.getMemoryLog().add(0, logMessage);
                while (agent.getMemoryLog().size() > 10) {
                    agent.getMemoryLog().remove(agent.getMemoryLog().size() - 1);
                }
            }

            executeNextActionStep(agent);
            agent.consumeThinkingStep();
            agentRepository.save(agent);
        } catch (Exception e) {
            log.error("Error during AI thinking process for agent {}: {}", agent.getName(), e.getMessage(), e);
            agent.updateStatus(AgentStatus.IDLE, "AI had trouble thinking (" + e.getMessage() + "). Standing by.");
            agentRepository.save(agent);
        }
    }

    public void executeNextActionStep(Agent agent) {
        if (!agent.hasActions()) {
            return;
        }

        ActionStep nextStep = agent.popNextAction();
        ActionResolverService.ResolvedTarget resolved = actionResolverService.resolve(agent, nextStep);

        agent.startMovement(resolved.x(), resolved.y(), nextStep.getActionSummary());
        
        try {
            agent.updateStatus(resolved.status(), nextStep.getActionSummary());
        } catch (Exception e) {
            log.error("Failed to update status: {}", e.getMessage());
        }

        String aiLog = "[AI] " + nextStep.getActionSummary() + " (" + nextStep.getActionType() + 
                (nextStep.getDirection() != null ? " " + nextStep.getDirection() + "[" + nextStep.getSteps() + "]" : "") +
                (nextStep.getRawX() != null ? " na (" + nextStep.getRawX() + "," + nextStep.getRawY() + ")" : "") + ")";
        eventPublisher.publishEvent(new AgentConsoleLogEvent(agent.getId(), aiLog));

        if (agent.getStatus() == AgentStatus.MOVING) {
            worldStateSynchronizer.syncMovementAfterCommit(agent);
        }
    }

    private <E extends Enum<E>> E safeParseEnum(String value, Class<E> enumType, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
