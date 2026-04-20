package com.agentgierka.mmo.creature.service;

import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.creature.event.CreatureSpawnedEvent;
import com.agentgierka.mmo.creature.model.*;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.repository.SpawnPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpawnService {

    private final SpawnPointRepository spawnPointRepository;
    private final CreatureInstanceRepository creatureInstanceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LootService lootService;
    private final Random random;

    @Transactional
    public void spawnAllActivePoints() {
        log.info("Spawning all active creatures from templates...");
        List<SpawnPoint> activePoints = spawnPointRepository.findAllByActiveTrue();
        activePoints.forEach(this::spawnAtPoint);
    }

    public void processRespawns() {
        List<CreatureInstance> deadCreatures = creatureInstanceRepository.findAllDead();
        Instant now = Instant.now();

        for (CreatureInstance dead : deadCreatures) {
            long secondsSinceDeath = Duration.between(dead.getDiedAt(), now).getSeconds();
            if (secondsSinceDeath >= dead.getRespawnSeconds()) {
                respawn(dead);
            }
        }
    }

    void spawnAtPoint(SpawnPoint point) {
        CreatureTemplate template = point.getCreatureTemplate();
        
        int posX = point.getCenterX() + getRandomOffset(point.getWanderRadius());
        int posY = point.getCenterY() + getRandomOffset(point.getWanderRadius());

        CreatureInstance instance = CreatureInstance.builder()
                .instanceId(UUID.randomUUID())
                .templateId(template.getId())
                .spawnPointId(point.getId())
                .locationId(point.getLocation().getId())
                .name(template.getName())
                .rank(template.getRank())
                .x(posX)
                .y(posY)
                .level(template.getLevel())
                .currentHp(template.getScaledHp())
                .maxHp(template.getScaledHp())
                .damage(template.getScaledDamage())
                .aggroRadius(template.getAggroRadius())
                .experienceReward(template.getScaledExperience())
                .state(CreatureState.ALIVE)
                .respawnSeconds(point.getRespawnSeconds())
                .build();

        creatureInstanceRepository.save(instance);
        eventPublisher.publishEvent(new CreatureSpawnedEvent(instance.getInstanceId(), instance.getLocationId(), instance));
        log.debug("Spawned {} at ({}, {}) in {}", instance.getName(), posX, posY, point.getLocation().getName());
    }

    @Transactional
    public List<String> killCreature(UUID instanceId) {
        CreatureInstance instance = creatureInstanceRepository.findById(instanceId);
        if (instance == null || instance.isDead()) {
            return List.of();
        }

        log.info("Creature {} killed.", instanceId);
        instance.kill();
        creatureInstanceRepository.save(instance);

        List<String> drops = lootService.rollLoot(instance.getTemplateId(), instance.getLocationId());

        eventPublisher.publishEvent(new CreatureKilledEvent(
                instance.getInstanceId(),
                instance.getLocationId(),
                instance.getTemplateId(),
                drops
        ));

        return drops;
    }

    private void respawn(CreatureInstance oldInstance) {
        spawnPointRepository.findById(oldInstance.getSpawnPointId()).ifPresent(point -> {
            log.info("Respawning creature: {}", oldInstance.getName());
            
            int posX = point.getCenterX() + getRandomOffset(point.getWanderRadius());
            int posY = point.getCenterY() + getRandomOffset(point.getWanderRadius());
            
            oldInstance.respawn(posX, posY);
            creatureInstanceRepository.save(oldInstance);
            
            eventPublisher.publishEvent(new CreatureSpawnedEvent(oldInstance.getInstanceId(), oldInstance.getLocationId(), oldInstance));
            log.debug("Respawned {} at ({}, {}) in {}", oldInstance.getName(), posX, posY, point.getLocation().getName());
        });
    }

    private int getRandomOffset(int radius) {
        if (radius <= 0) return 0;
        return random.nextInt(2 * radius + 1) - radius;
    }
}
