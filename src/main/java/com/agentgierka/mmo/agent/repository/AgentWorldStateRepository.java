package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
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

    /**
     * Updates the agent state atomically using optimistic locking.
     * Returns true if update was successful, false if version was stale or record missing.
     */
    private static final String ATOMIC_UPDATE_LUA =
            "local current = redis.call('GET', KEYS[1]) " +
            "if not current then return -1 end " +
            "if current:find('\"version\":' .. ARGV[1], 1, true) then " +
            "  redis.call('SET', KEYS[1], ARGV[2]) " +
            "  if ARGV[3] == 'MOVING' then " +
            "    redis.call('SADD', KEYS[2], ARGV[4]) " +
            "  else " +
            "    redis.call('SREM', KEYS[2], ARGV[4]) " +
            "  end " +
            "  return 1 " +
            "end " +
            "return 0";

    public boolean updateAtomic(AgentWorldState newState) {
        long expectedVersion = newState.getVersion();
        newState.incrementVersion();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ATOMIC_UPDATE_LUA, Long.class);

        Long result = mmoStringRedisTemplate.execute(
                script,
                List.of(KEY_PREFIX + newState.getAgentId(), ACTIVE_AGENTS_SET),
                String.valueOf(expectedVersion),
                serializeToJson(newState),
                newState.getStatus().name(),
                newState.getAgentId().toString()
        );

        return result != null && result == 1L;
    }

    @SuppressWarnings("unchecked")
    private String serializeToJson(AgentWorldState state) {
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) agentWorldStateTemplate.getValueSerializer();
        byte[] serialized = serializer.serialize(state);
        if (serialized == null) {
            throw new IllegalStateException("Failed to serialize AgentWorldState");
        }
        return new String(serialized, StandardCharsets.UTF_8);
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

    /**
     * Clears all agent-related data from Redis. Use with caution.
     */
    public void deleteAll() {
        Set<String> keys = agentWorldStateTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            agentWorldStateTemplate.delete(keys);
        }
        mmoStringRedisTemplate.delete(ACTIVE_AGENTS_SET);
    }
}
