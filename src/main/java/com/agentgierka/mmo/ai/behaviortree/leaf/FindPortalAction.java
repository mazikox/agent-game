package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.world.Portal;

import java.util.List;

public class FindPortalAction implements BehaviorNode {
    private final String targetLocationName;

    public FindPortalAction(String targetLocationName) {
        this.targetLocationName = targetLocationName;
    }

    @Override
    public NodeStatus tick(BehaviorContext context) {
        Agent agent = context.agent();
        if (agent.getCurrentLocation() == null) {
            return NodeStatus.FAILURE;
        }

        List<Portal> portals = context.portalRepository()
                .findAllBySourceLocationIdWithTarget(agent.getCurrentLocation().getId());

        Portal matchingPortal = portals.stream()
                .filter(p -> p.getTargetLocation() != null && 
                        p.getTargetLocation().getName().equalsIgnoreCase(targetLocationName))
                .findFirst()
                .orElse(null);

        if (matchingPortal == null) {
            return NodeStatus.FAILURE;
        }

        agent.targetEntity(matchingPortal.getId());
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        // Leaf actions hold no state
    }

    @Override
    public String describe() {
        return "FindPortal[" + targetLocationName + "]";
    }
}
