package com.agentgierka.mmo.creature.service;

import com.agentgierka.mmo.creature.event.CreatureSpawnedEvent;
import com.agentgierka.mmo.creature.model.*;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.repository.SpawnPointRepository;
import com.agentgierka.mmo.world.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpawnServiceTest {

    @Mock
    private SpawnPointRepository spawnPointRepository;
    @Mock
    private CreatureInstanceRepository creatureInstanceRepository;
    @Mock
    private LootService lootService;
    @Mock
    private Random random;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SpawnService spawnService;

    @Test
    void shouldRespawnCreatureWhenTimePassed() {
        UUID spawnPointId = UUID.randomUUID();
        CreatureInstance deadCreature = CreatureInstance.builder()
                .instanceId(UUID.randomUUID())
                .spawnPointId(spawnPointId)
                .diedAt(Instant.now().minusSeconds(70))
                .respawnSeconds(60)
                .state(CreatureState.DEAD)
                .build();

        SpawnPoint spawnPoint = SpawnPoint.builder()
                .id(spawnPointId)
                .creatureTemplate(CreatureTemplate.builder().name("Forest Wolf").build())
                .location(Location.builder().id(UUID.randomUUID()).name("Forest").build())
                .respawnSeconds(60)
                .build();

        when(creatureInstanceRepository.findAllDead()).thenReturn(List.of(deadCreature));
        when(spawnPointRepository.findById(spawnPointId)).thenReturn(Optional.of(spawnPoint));

        spawnService.processRespawns();

        verify(creatureInstanceRepository, never()).delete(any());
        verify(creatureInstanceRepository).save(deadCreature);
        verify(eventPublisher).publishEvent(any(CreatureSpawnedEvent.class));
        
        assert deadCreature.getState() == CreatureState.ALIVE;
        assert deadCreature.getDiedAt() == null;
    }

    @Test
    void shouldNotRespawnIfTimeNotPassed() {
        UUID spawnPointId = UUID.randomUUID();
        CreatureInstance deadCreature = CreatureInstance.builder()
                .spawnPointId(spawnPointId)
                .diedAt(Instant.now().minusSeconds(59))
                .respawnSeconds(60)
                .state(CreatureState.DEAD)
                .build();

        when(creatureInstanceRepository.findAllDead()).thenReturn(List.of(deadCreature));

        spawnService.processRespawns();

        verify(creatureInstanceRepository, never()).save(any());
    }

    @Test
    void shouldKeepSpawnWithinWanderRadius() {
        CreatureTemplate template = CreatureTemplate.builder().level(1).baseHp(1).aggroRadius(1).build();
        Location location = Location.builder().id(UUID.randomUUID()).build();
        SpawnPoint point = SpawnPoint.builder()
                .id(UUID.randomUUID())
                .creatureTemplate(template)
                .location(location)
                .centerX(100)
                .centerY(100)
                .wanderRadius(10)
                .build();

        for (int i = 0; i < 100; i++) {
            spawnService.spawnAtPoint(point);
        }

        // Verify that all saved instances are within range [90, 110]
        verify(creatureInstanceRepository, times(100)).save(argThat(instance -> 
            instance.getX() >= 90 && instance.getX() <= 110 &&
            instance.getY() >= 90 && instance.getY() <= 110
        ));
    }

    @Test
    void shouldKillCreatureAndPublishEvent() {
        UUID instanceId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        CreatureInstance instance = CreatureInstance.builder()
                .instanceId(instanceId)
                .locationId(locationId)
                .templateId(templateId)
                .maxHp(100)
                .currentHp(100)
                .state(CreatureState.ALIVE)
                .build();

        List<String> expectedDrops = List.of("Gold Coin");
        when(creatureInstanceRepository.findById(instanceId)).thenReturn(instance);
        when(lootService.rollLoot(templateId, locationId)).thenReturn(expectedDrops);

        List<String> actualDrops = spawnService.killCreature(instanceId);

        verify(creatureInstanceRepository).save(argThat(i -> i.getState() == CreatureState.DEAD));
        verify(eventPublisher).publishEvent(any(com.agentgierka.mmo.creature.event.CreatureKilledEvent.class));
        assert actualDrops.equals(expectedDrops);
    }
}
