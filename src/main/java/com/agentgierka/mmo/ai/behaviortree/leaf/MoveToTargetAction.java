package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.world.Portal;

import java.util.Optional;
import java.util.UUID;

public class MoveToTargetAction implements BehaviorNode {

    @Override
    public NodeStatus tick(BehaviorContext context) {
        Agent agent = context.agent();
        UUID targetId = agent.getTargetId();

        if (targetId == null) {
            return NodeStatus.FAILURE;
        }

        Integer targetX = null;
        Integer targetY = null;

        CreatureInstance creature = context.creatureRepository().findById(targetId);
        if (creature != null) {
            targetX = creature.getX();
            targetY = creature.getY();
        } else {
            Optional<Portal> portal = context.portalRepository().findById(targetId);
            if (portal.isPresent()) {
                targetX = portal.get().getSourceX();
                targetY = portal.get().getSourceY();
            }
        }

        if (targetX == null || targetY == null) {
            return NodeStatus.FAILURE;
        }

        int agentX = agent.getX() != null ? agent.getX() : 0;
        int agentY = agent.getY() != null ? agent.getY() : 0;

        if (agentX == targetX && agentY == targetY) {
            return NodeStatus.SUCCESS;
        }

        if (agent.getStatus() == AgentStatus.MOVING 
                && targetX.equals(agent.getTargetX()) 
                && targetY.equals(agent.getTargetY())) {
            return NodeStatus.RUNNING;
        }

        agent.startMovement(targetX, targetY, "Moving to target");
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        // Leaf actions hold no state
    }

    @Override
    public String describe() {
        return "MoveToTarget";
    }
}
