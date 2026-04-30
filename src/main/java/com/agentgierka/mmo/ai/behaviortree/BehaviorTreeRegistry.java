package com.agentgierka.mmo.ai.behaviortree;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Component
public class BehaviorTreeRegistry {
    private final Cache<UUID, BehaviorNode> activeTrees = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    public void register(UUID agentId, BehaviorNode tree) {
        activeTrees.put(agentId, tree);
    }

    public Optional<BehaviorNode> get(UUID agentId) {
        return Optional.ofNullable(activeTrees.getIfPresent(agentId));
    }

    public void remove(UUID agentId) {
        activeTrees.invalidate(agentId);
    }
}
