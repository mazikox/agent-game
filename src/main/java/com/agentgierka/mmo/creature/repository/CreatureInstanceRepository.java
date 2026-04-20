package com.agentgierka.mmo.creature.repository;

import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CreatureInstanceRepository {

    private final RedisTemplate<String, CreatureInstance> creatureInstanceTemplate;
    private final RedisTemplate<String, String> mmoStringRedisTemplate;

    private static final String KEY_PREFIX = "creature:instance:";
    private static final String LOCATION_SET_PREFIX = "creature:location:";
    private static final String DEAD_CREATURES_SET = "creature:dead_ids";

    public void save(CreatureInstance instance) {
        String key = KEY_PREFIX + instance.getInstanceId();

        String locationKey = LOCATION_SET_PREFIX + instance.getLocationId();
        mmoStringRedisTemplate.opsForSet().add(locationKey, instance.getInstanceId().toString());

        if (instance.getState() == CreatureState.DEAD) {
            long ttlSeconds = Math.max(60, instance.getRespawnSeconds() * 2L);
            creatureInstanceTemplate.opsForValue().set(key, instance, java.time.Duration.ofSeconds(ttlSeconds));
            mmoStringRedisTemplate.opsForSet().add(DEAD_CREATURES_SET, instance.getInstanceId().toString());
        } else {
            creatureInstanceTemplate.opsForValue().set(key, instance);
            mmoStringRedisTemplate.opsForSet().remove(DEAD_CREATURES_SET, instance.getInstanceId().toString());
        }
    }

    public CreatureInstance findById(UUID instanceId) {
        return creatureInstanceTemplate.opsForValue().get(KEY_PREFIX + instanceId);
    }

    public List<CreatureInstance> findAllByLocationId(UUID locationId) {
        String locationKey = LOCATION_SET_PREFIX + locationId;
        Set<String> ids = mmoStringRedisTemplate.opsForSet().members(locationKey);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<CreatureInstance> results = creatureInstanceTemplate.opsForValue().multiGet(keys);
        if (results == null) return Collections.emptyList();
        
        return results.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<CreatureInstance> findAllDead() {
        Set<String> ids = mmoStringRedisTemplate.opsForSet().members(DEAD_CREATURES_SET);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<CreatureInstance> results = creatureInstanceTemplate.opsForValue().multiGet(keys);
        if (results == null) return Collections.emptyList();

        return results.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static final String ATOMIC_UPDATE_LUA =
            "local current = redis.call('GET', KEYS[1]) " +
            "if not current then return -1 end " +
            "local data = cjson.decode(current) " +
            "if data.version == tonumber(ARGV[1]) then " +
            "  if ARGV[3] == 'DEAD' then " +
            "    redis.call('SET', KEYS[1], ARGV[2], 'EX', tonumber(ARGV[5])) " +
            "    redis.call('SADD', KEYS[2], ARGV[4]) " +
            "  else " +
            "    redis.call('SET', KEYS[1], ARGV[2]) " +
            "    redis.call('SREM', KEYS[2], ARGV[4]) " +
            "  end " +
            "  return 1 " +
            "end " +
            "return 0";

    public boolean updateAtomic(CreatureInstance newState) {
        long expectedVersion = newState.getVersion();
        newState.incrementVersion();

        org.springframework.data.redis.core.script.DefaultRedisScript<Long> script = 
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(ATOMIC_UPDATE_LUA, Long.class);

        long ttlSeconds = Math.max(60, newState.getRespawnSeconds() * 2L);

        Long result = mmoStringRedisTemplate.execute(
                script,
                List.of(KEY_PREFIX + newState.getInstanceId(), DEAD_CREATURES_SET),
                String.valueOf(expectedVersion),
                serializeToJson(newState),
                newState.getState().name(),
                newState.getInstanceId().toString(),
                String.valueOf(ttlSeconds)
        );

        return result != null && result == 1L;
    }

    @SuppressWarnings("unchecked")
    private String serializeToJson(CreatureInstance state) {
        org.springframework.data.redis.serializer.RedisSerializer<Object> serializer = 
                (org.springframework.data.redis.serializer.RedisSerializer<Object>) creatureInstanceTemplate.getValueSerializer();
        byte[] serialized = serializer.serialize(state);
        if (serialized == null) {
            throw new IllegalStateException("Failed to serialize CreatureInstance");
        }
        return new String(serialized, java.nio.charset.StandardCharsets.UTF_8);
    }

    public void delete(CreatureInstance instance) {
        creatureInstanceTemplate.delete(KEY_PREFIX + instance.getInstanceId());
        mmoStringRedisTemplate.opsForSet().remove(LOCATION_SET_PREFIX + instance.getLocationId(), instance.getInstanceId().toString());
        mmoStringRedisTemplate.opsForSet().remove(DEAD_CREATURES_SET, instance.getInstanceId().toString());
    }

    public void deleteAll() {
        deleteKeysByPattern(KEY_PREFIX + "*");
        deleteKeysByPattern(LOCATION_SET_PREFIX + "*");
        mmoStringRedisTemplate.delete(DEAD_CREATURES_SET);
    }

    private void deleteKeysByPattern(String pattern) {
        mmoStringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            try (var cursor = connection.keyCommands().scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    connection.keyCommands().del(cursor.next());
                }
            }
            return null;
        });
    }
}
