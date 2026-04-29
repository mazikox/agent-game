package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;

import java.util.UUID;

public class AttackTargetAction implements BehaviorNode {

    @Override
    public NodeStatus tick(BehaviorContext context) {
        Agent agent = context.agent();
        UUID targetId = agent.getTargetId();

        if (targetId == null) {
            return NodeStatus.FAILURE;
        }

        CreatureInstance creature = context.creatureRepository().findById(targetId);
        if (creature == null) {
            return NodeStatus.FAILURE;
        }

        if (creature.getState() == CreatureState.DEAD) {
            agent.clearTarget();
            return NodeStatus.SUCCESS;
        }

        agent.updateStatus(agent.getStatus(), "Waiting for player to defeat: " + creature.getName());
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        // Leaf actions hold no state
    }

    @Override
    public String describe() {
        return "AttackTarget";
    }
}
