package com.agentgierka.mmo.ai.behaviortree.condition;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Registry holding GoalProgress for active agents.
 * Similar to BehaviorTreeRegistry, it manages lifecycle of progress tracking state.
 */
@Component
public class GoalProgressRegistry {

    private final Cache<UUID, GoalProgress> store = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .build();

    public GoalProgress getOrCreate(UUID agentId) {
        return store.get(agentId, k -> new GoalProgress());
    }

    public GoalProgress getIfExists(UUID agentId) {
        return store.getIfPresent(agentId);
    }

    public void remove(UUID agentId) {
        store.invalidate(agentId);
    }

    public void clear() {
        store.invalidateAll();
    }
}
