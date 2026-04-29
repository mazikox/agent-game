package com.agentgierka.mmo.ai.behaviortree.leaf;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;

import java.util.Comparator;
import java.util.List;

public class FindNearestCreatureAction implements BehaviorNode {

    @Override
    public NodeStatus tick(BehaviorContext context) {
        if (context.agent().getCurrentLocation() == null) {
            return NodeStatus.FAILURE;
        }

        List<CreatureInstance> aliveCreatures = context.creatureRepository()
                .findAllByLocationId(context.agent().getCurrentLocation().getId())
                .stream()
                .filter(c -> c.getState() != CreatureState.DEAD)
                .toList();

        if (aliveCreatures.isEmpty()) {
            return NodeStatus.FAILURE;
        }

        int agentX = context.agent().getX() != null ? context.agent().getX() : 0;
        int agentY = context.agent().getY() != null ? context.agent().getY() : 0;

        CreatureInstance nearest = aliveCreatures.stream()
                .min(Comparator.comparingDouble(c -> 
                        Math.sqrt(Math.pow(c.getX() - agentX, 2) + Math.pow(c.getY() - agentY, 2))))
                .orElse(aliveCreatures.get(0));

        context.agent().targetEntity(nearest.getInstanceId());
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        // Leaf actions hold no state
    }

    @Override
    public String describe() {
        return "FindNearestCreature";
    }
}
