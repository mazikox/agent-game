package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
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
            return isCombatFinished(agent) ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
        }

        CreatureInstance creature = context.creatureRepository().findById(targetId);
        if (creature == null || creature.isDead()) {
            agent.clearTarget();
            return NodeStatus.SUCCESS;
        }

        return processCombat(agent, creature, context);
    }

    private boolean isCombatFinished(Agent agent) {
        return agent.getStatus() != AgentStatus.IN_COMBAT;
    }

    private NodeStatus processCombat(Agent agent, CreatureInstance creature, BehaviorContext context) {
        if (agent.getStatus() != AgentStatus.IN_COMBAT) {
            try {
                context.combatService().initiateCombat(agent.getId(), creature.getInstanceId());
            } catch (Exception e) {
                agent.updateStatus(agent.getStatus(), "Waiting for target to be available: " + creature.getName());
                return NodeStatus.RUNNING;
            }
        }

        agent.updateStatus(AgentStatus.IN_COMBAT, "Engaged in combat with: " + creature.getName());
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
