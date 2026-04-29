package com.agentgierka.mmo.ai.behaviortree;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BehaviorTreeRegistry {
    private final ConcurrentHashMap<UUID, BehaviorNode> activeTrees = new ConcurrentHashMap<>();

    public void register(UUID agentId, BehaviorNode tree) {
        activeTrees.put(agentId, tree);
    }

    public Optional<BehaviorNode> get(UUID agentId) {
        return Optional.ofNullable(activeTrees.get(agentId));
    }

    public void remove(UUID agentId) {
        activeTrees.remove(agentId);
    }
}
