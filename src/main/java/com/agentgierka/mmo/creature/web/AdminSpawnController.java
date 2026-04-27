package com.agentgierka.mmo.creature.web;

import com.agentgierka.mmo.creature.model.CreatureTemplate;
import com.agentgierka.mmo.creature.model.SpawnPoint;
import com.agentgierka.mmo.creature.repository.CreatureTemplateRepository;
import com.agentgierka.mmo.creature.repository.SpawnPointRepository;
import com.agentgierka.mmo.creature.service.SpawnService;
import com.agentgierka.mmo.creature.web.dto.CreateSpawnPointRequest;
import com.agentgierka.mmo.creature.web.dto.CreatureTemplateDto;
import com.agentgierka.mmo.creature.web.dto.SpawnPointDto;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.creature.web.mapper.SpawnPointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSpawnController {

    private final CreatureTemplateRepository creatureTemplateRepository;
    private final SpawnPointRepository spawnPointRepository;
    private final LocationRepository locationRepository;
    private final SpawnService spawnService;
    private final SpawnPointMapper spawnPointMapper;

    @GetMapping("/creature-templates")
    public List<CreatureTemplateDto> getCreatureTemplates() {
        return creatureTemplateRepository.findAll().stream()
                .map(spawnPointMapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/spawn-points")
    public ResponseEntity<SpawnPointDto> createSpawnPoint(@RequestBody CreateSpawnPointRequest request) {
        CreatureTemplate template = creatureTemplateRepository.findById(request.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Creature template not found: " + request.templateId()));
        
        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + request.locationId()));

        SpawnPoint newPoint = SpawnPoint.create(
                template,
                location,
                request.centerX(),
                request.centerY(),
                request.wanderRadius(),
                request.respawnSeconds()
        );

        SpawnPoint savedPoint = spawnPointRepository.save(newPoint);
        spawnService.spawnAtPoint(savedPoint);

        return ResponseEntity.status(HttpStatus.CREATED).body(spawnPointMapper.toDto(savedPoint));
    }

    @DeleteMapping("/spawn-points/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpawnPoint(@PathVariable UUID id) {
        spawnPointRepository.deleteById(id);
    }
}
