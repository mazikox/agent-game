package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.ai.model.Direction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MoveDirectionAction implements BehaviorNode {

    private final Direction direction;
    private final int steps;

    @Override
    public NodeStatus tick(BehaviorContext context) {
        Agent agent = context.agent();

        if (direction == null || steps <= 0) {
            return NodeStatus.FAILURE;
        }

        Agent.Point target = agent.calculateTarget(direction, steps);
        
        int agentX = agent.getX() != null ? agent.getX() : 0;
        int agentY = agent.getY() != null ? agent.getY() : 0;

        if (agentX == target.x() && agentY == target.y()) {
            return NodeStatus.SUCCESS;
        }

        if (agent.getStatus() == AgentStatus.MOVING 
                && Integer.valueOf(target.x()).equals(agent.getTargetX()) 
                && Integer.valueOf(target.y()).equals(agent.getTargetY())) {
            return NodeStatus.RUNNING;
        }

        agent.startMovement(target.x(), target.y(), "Moving " + direction + " by " + steps + " steps");
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
    }

    @Override
    public String describe() {
        return "MoveDirection(" + direction + ", " + steps + ")";
    }
}
