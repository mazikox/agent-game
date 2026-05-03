package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.world.Portal;

import java.util.Optional;
import java.util.UUID;

public class EnterPortalAction implements BehaviorNode {

    @Override
    public NodeStatus tick(BehaviorContext context) {
        Agent agent = context.agent();
        UUID targetId = agent.getTargetId();

        if (targetId == null) {
            return NodeStatus.FAILURE;
        }

        Optional<Portal> portal = context.portalRepository().findById(targetId);
        if (portal.isEmpty()) {
            return NodeStatus.FAILURE;
        }

        Portal p = portal.get();
        int agentX = agent.getX() != null ? agent.getX() : 0;
        int agentY = agent.getY() != null ? agent.getY() : 0;

        int portalX = p.getSourceX() != null ? p.getSourceX() : 0;
        int portalY = p.getSourceY() != null ? p.getSourceY() : 0;

        if (agentX == portalX && agentY == portalY) {
            agent.teleport(p.getTargetLocation(), p.getTargetX(), p.getTargetY());
            agent.clearTarget();
            return NodeStatus.SUCCESS;
        }

        // Agent is not at portal yet - move towards it
        if (agent.getStatus() == com.agentgierka.mmo.agent.model.AgentStatus.MOVING 
                && Integer.valueOf(portalX).equals(agent.getTargetX()) 
                && Integer.valueOf(portalY).equals(agent.getTargetY())) {
            return NodeStatus.RUNNING;
        }

        agent.startMovement(portalX, portalY, "Moving to portal to " + p.getTargetLocation().getName());
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        // Leaf actions hold no state
    }

    @Override
    public String describe() {
        return "EnterPortal";
    }
}
