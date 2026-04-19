package com.agentgierka.mmo.creature;

import com.agentgierka.mmo.creature.model.*;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.repository.CreatureTemplateRepository;
import com.agentgierka.mmo.creature.repository.SpawnPointRepository;
import com.agentgierka.mmo.creature.service.LootService;
import com.agentgierka.mmo.creature.service.SpawnService;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.LocationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreatureIntegrationTest {

    @Autowired
    private SpawnService spawnService;
    @Autowired
    private LootService lootService;
    @Autowired
    private CreatureTemplateRepository templateRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private SpawnPointRepository spawnPointRepository;
    @MockitoBean
    private CreatureInstanceRepository instanceRepository;

    private final List<CreatureInstance> fakeRedisStore = Collections.synchronizedList(new ArrayList<>());

    @org.junit.jupiter.api.BeforeEach
    void setUpMocks() {
        fakeRedisStore.clear();

        // Stub save
        doAnswer(invocation -> {
            CreatureInstance instance = invocation.getArgument(0);
            fakeRedisStore.removeIf(i -> i.getInstanceId().equals(instance.getInstanceId()));
            fakeRedisStore.add(instance);
            return null;
        }).when(instanceRepository).save(any(CreatureInstance.class));

        // Stub findById
        when(instanceRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return fakeRedisStore.stream()
                    .filter(i -> i.getInstanceId().equals(id))
                    .findFirst()
                    .orElse(null);
        });

        // Stub findAllByLocationId
        when(instanceRepository.findAllByLocationId(any(UUID.class))).thenAnswer(invocation -> {
            UUID locId = invocation.getArgument(0);
            return fakeRedisStore.stream()
                    .filter(i -> i.getLocationId().equals(locId))
                    .toList();
        });

        // Stub findAllDead
        when(instanceRepository.findAllDead()).thenAnswer(invocation -> 
            fakeRedisStore.stream()
                    .filter(CreatureInstance::isDead)
                    .toList()
        );

        // Stub delete
        doAnswer(invocation -> {
            CreatureInstance instance = invocation.getArgument(0);
            fakeRedisStore.removeIf(i -> i.getInstanceId().equals(instance.getInstanceId()));
            return null;
        }).when(instanceRepository).delete(any(CreatureInstance.class));
    }

    @Test
    void testFullCreatureLifecycle() {
        // 1. Setup Data
        Location loc = Location.builder().name("Test Loc").type(LocationType.FOREST).width(10).height(10).build();
        locationRepository.save(loc);
        
        CreatureTemplate wolf = CreatureTemplate.create("Test Wolf", CreatureRank.NORMAL, 1, 100, 10, 10, 5);
        templateRepository.save(wolf);
        
        SpawnPoint point = SpawnPoint.create(wolf, loc, 5, 5, 0, 1); // 1s respawn
        spawnPointRepository.save(point);

        // 2. Initial Spawn
        spawnService.spawnAllActivePoints();
        List<CreatureInstance> alive = instanceRepository.findAllByLocationId(loc.getId());
        assertThat(alive).hasSize(1);
        CreatureInstance instance = alive.get(0);

        // 3. Kill
        List<String> loot = spawnService.killCreature(instance.getInstanceId());
        assertThat(loot).isNotNull();

        // 4. Respawn processing (wait for respawn time to expire)
        try { Thread.sleep(1100); } catch (InterruptedException e) {}
        spawnService.processRespawns();
        
        List<CreatureInstance> result = instanceRepository.findAllByLocationId(loc.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getState()).isEqualTo(CreatureState.ALIVE);
        assertThat(result.get(0).getInstanceId()).isNotEqualTo(instance.getInstanceId());
    }
}
