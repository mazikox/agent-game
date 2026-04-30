package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MoveToPositionAction implements BehaviorNode {

    private final int rawX;
    private final int rawY;

    @Override
    public NodeStatus tick(BehaviorContext context) {
        Agent agent = context.agent();

        int agentX = agent.getX() != null ? agent.getX() : 0;
        int agentY = agent.getY() != null ? agent.getY() : 0;

        if (agentX == rawX && agentY == rawY) {
            return NodeStatus.SUCCESS;
        }

        if (agent.getStatus() == AgentStatus.MOVING 
                && Integer.valueOf(rawX).equals(agent.getTargetX()) 
                && Integer.valueOf(rawY).equals(agent.getTargetY())) {
            return NodeStatus.RUNNING;
        }

        agent.startMovement(rawX, rawY, "Moving to position (" + rawX + ", " + rawY + ")");
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
    }

    @Override
    public String describe() {
        return "MoveToPosition(" + rawX + ", " + rawY + ")";
    }
}
