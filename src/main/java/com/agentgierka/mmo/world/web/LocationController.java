package com.agentgierka.mmo.world.web;

import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.world.exception.LocationNotFoundException;
import com.agentgierka.mmo.world.web.dto.LocationDto;
import com.agentgierka.mmo.world.web.dto.PortalDto;
import com.agentgierka.mmo.world.web.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for world-related data. 
 * Provides map layouts and teleportation portal locations.
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepository;
    private final PortalRepository portalRepository;
    private final LocationMapper locationMapper;

    /**
     * Returns location details including map boundaries (width/height) 
     * and a list of portals present in this location.
     */
    @GetMapping("/{id}")
    public LocationDto get(@PathVariable UUID id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id.toString()));

        
        LocationDto dto = locationMapper.toDto(location);
        
        // Populate portals for this source location
        List<PortalDto> portals = portalRepository.findAllBySourceLocationIdWithTarget(id)
                .stream()
                .map(p -> PortalDto.builder()
                        .id(p.getId())
                        .sourceX(p.getSourceX())
                        .sourceY(p.getSourceY())
                        .targetLocationName(p.getTargetLocation().getName())
                        .build())
                .collect(Collectors.toList());
        
        dto.setPortals(portals);
        return dto;
    }
}
