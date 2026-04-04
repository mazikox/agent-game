package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages real-time agent state in Redis.
 */
@Repository
@RequiredArgsConstructor
public class AgentWorldStateRepository {

    private final RedisTemplate<String, AgentWorldState> agentWorldStateTemplate;
    private final RedisTemplate<String, String> mmoStringRedisTemplate;
    private static final String KEY_PREFIX = "agent:state:";
    private static final String ACTIVE_AGENTS_SET = "agent:active_ids";

    public void save(AgentWorldState state) {
        String key = KEY_PREFIX + state.getAgentId();
        agentWorldStateTemplate.opsForValue().set(key, state);
        
        if (state.getStatus() == AgentStatus.MOVING) {
            mmoStringRedisTemplate.opsForSet().add(ACTIVE_AGENTS_SET, state.getAgentId().toString());
        } else {
            mmoStringRedisTemplate.opsForSet().remove(ACTIVE_AGENTS_SET, state.getAgentId().toString());
        }
    }

    public void saveAll(List<AgentWorldState> states) {
        if (states.isEmpty()) {
            return;
        }

        Map<String, AgentWorldState> keyValues = states.stream()
                .collect(Collectors.toMap(
                        state -> KEY_PREFIX + state.getAgentId(),
                        state -> state
                ));

        agentWorldStateTemplate.opsForValue().multiSet(keyValues);

        for (AgentWorldState state : states) {
            if (state.getStatus() == AgentStatus.MOVING) {
                mmoStringRedisTemplate.opsForSet().add(ACTIVE_AGENTS_SET, state.getAgentId().toString());
            } else {
                mmoStringRedisTemplate.opsForSet().remove(ACTIVE_AGENTS_SET, state.getAgentId().toString());
            }
        }
    }

    public AgentWorldState findById(UUID agentId) {
        return agentWorldStateTemplate.opsForValue().get(KEY_PREFIX + agentId);
    }

    public List<AgentWorldState> findAllActive() {
        Set<String> activeIds = mmoStringRedisTemplate.opsForSet().members(ACTIVE_AGENTS_SET);
        if (activeIds == null || activeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = activeIds.stream()
                .map(id -> KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<AgentWorldState> results = agentWorldStateTemplate.opsForValue().multiGet(keys);
        if (results == null) {
            return Collections.emptyList();
        }

        return results.stream()
                .filter(state -> state != null && state.getStatus() == AgentStatus.MOVING)
                .collect(Collectors.toList());
    }

    public void delete(UUID agentId) {
        agentWorldStateTemplate.delete(KEY_PREFIX + agentId);
        mmoStringRedisTemplate.opsForSet().remove(ACTIVE_AGENTS_SET, agentId.toString());
    }
}
