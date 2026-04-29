package com.agentgierka.mmo.agent.service;

import com.agentgierka.mmo.agent.model.ActionStep;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.model.ActionType;
import com.agentgierka.mmo.ai.model.QualifierType;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.world.Portal;
import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActionResolverService {

    private final CreatureInstanceRepository creatureInstanceRepository;
    private final PortalRepository portalRepository;

    public record ResolvedTarget(Integer x, Integer y, AgentStatus status) {}

    public ResolvedTarget resolve(Agent agent, ActionStep step) {
        if (step.getActionType() == null) {
            return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
        }

        return switch (step.getActionType()) {
            case MOVE_TO_CREATURE -> resolveCreature(agent, step);
            case MOVE_TO_PORTAL -> resolvePortal(agent, step);
            case MOVE_TO_POSITION -> resolvePosition(agent, step);
            case MOVE_RELATIVE -> resolveRelative(agent, step);
            case MOVE_DIRECTION -> resolveDirection(agent, step);
            case IDLE -> new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
            default -> new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
        };
    }

    private ResolvedTarget resolveCreature(Agent agent, ActionStep step) {
        List<CreatureInstance> creatures = creatureInstanceRepository.findAllByLocationId(agent.getCurrentLocation().getId())
                .stream()
                .filter(c -> c.getState() != CreatureState.DEAD)
                .toList();

        if (creatures.isEmpty()) {
            log.warn("No creatures found in location for agent {}", agent.getName());
            return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
        }

        if (step.getTargetIndex() != null && step.getTargetIndex() >= 0 && step.getTargetIndex() < creatures.size()) {
            CreatureInstance target = creatures.get(step.getTargetIndex());
            return new ResolvedTarget(target.getX(), target.getY(), AgentStatus.MOVING);
        }

        if (step.getQualifier() == QualifierType.NEAREST || step.getQualifier() == null) {
            CreatureInstance nearest = creatures.stream()
                    .min(Comparator.comparingDouble(c -> distance(agent.getX(), agent.getY(), c.getX(), c.getY())))
                    .orElse(creatures.get(0));
            return new ResolvedTarget(nearest.getX(), nearest.getY(), AgentStatus.MOVING);
        }

        if (step.getQualifier() == QualifierType.NEAREST_OTHER) {
            CreatureInstance nearestOther = creatures.stream()
                    .filter(c -> c.getX() != agent.getX() || c.getY() != agent.getY())
                    .min(Comparator.comparingDouble(c -> distance(agent.getX(), agent.getY(), c.getX(), c.getY())))
                    .orElse(creatures.get(0));
            return new ResolvedTarget(nearestOther.getX(), nearestOther.getY(), AgentStatus.MOVING);
        }

        return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
    }

    private ResolvedTarget resolvePortal(Agent agent, ActionStep step) {
        List<Portal> portals = portalRepository.findAllBySourceLocationId(agent.getCurrentLocation().getId());

        if (portals.isEmpty()) {
            log.warn("No portals found in location for agent {}", agent.getName());
            return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
        }

        if (step.getTargetIndex() != null && step.getTargetIndex() >= 0 && step.getTargetIndex() < portals.size()) {
            Portal target = portals.get(step.getTargetIndex());
            return new ResolvedTarget(target.getSourceX(), target.getSourceY(), AgentStatus.MOVING);
        }

        if (step.getQualifier() == QualifierType.NEAREST || step.getQualifier() == null) {
            Portal nearest = portals.stream()
                    .min(Comparator.comparingDouble(p -> distance(agent.getX(), agent.getY(), p.getSourceX(), p.getSourceY())))
                    .orElse(portals.get(0));
            return new ResolvedTarget(nearest.getSourceX(), nearest.getSourceY(), AgentStatus.MOVING);
        }

        return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
    }

    private ResolvedTarget resolvePosition(Agent agent, ActionStep step) {
        if (step.getRawX() != null && step.getRawY() != null) {
            return new ResolvedTarget(step.getRawX(), step.getRawY(), AgentStatus.MOVING);
        }
        return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
    }

    private ResolvedTarget resolveRelative(Agent agent, ActionStep step) {
        int newX = agent.getX() + (step.getRawX() != null ? step.getRawX() : 0);
        int newY = agent.getY() + (step.getRawY() != null ? step.getRawY() : 0);
        return new ResolvedTarget(newX, newY, AgentStatus.MOVING);
    }

    private ResolvedTarget resolveDirection(Agent agent, ActionStep step) {
        if (step.getDirection() != null) {
            Agent.Point target = agent.calculateTarget(step.getDirection(), step.getSteps());
            return new ResolvedTarget(target.x(), target.y(), AgentStatus.MOVING);
        }
        return new ResolvedTarget(agent.getX(), agent.getY(), AgentStatus.IDLE);
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}
