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
        creatureInstanceTemplate.opsForValue().set(key, instance);

        String locationKey = LOCATION_SET_PREFIX + instance.getLocationId();
        mmoStringRedisTemplate.opsForSet().add(locationKey, instance.getInstanceId().toString());

        if (instance.getState() == CreatureState.DEAD) {
            mmoStringRedisTemplate.opsForSet().add(DEAD_CREATURES_SET, instance.getInstanceId().toString());
        } else {
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
