package com.agentgierka.mmo.creature.web;

import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.creature.web.dto.CreatureDto;
import com.agentgierka.mmo.creature.web.mapper.CreatureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Creature interactions.
 * Nested under locations as creatures are bound to specific maps.
 */
@RestController
@RequestMapping("/api/v1/locations/{locationId}/creatures")
@RequiredArgsConstructor
public class CreatureController {

    private final CreatureInstanceRepository creatureInstanceRepository;
    private final CreatureMapper creatureMapper;

    /**
     * Retrieves all alive creatures in a specific location.
     */
    @GetMapping
    public List<CreatureDto> getCreaturesInLocation(@PathVariable UUID locationId) {
        return creatureInstanceRepository.findAllByLocationId(locationId).stream()
                .filter(c -> !c.isDead())
                .map(creatureMapper::toDto)
                .collect(Collectors.toList());
    }
}
