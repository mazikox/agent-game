package com.agentgierka.mmo.world.service;

import com.agentgierka.mmo.world.Portal;
import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain service responsible for detecting and handling interactions 
 * between agents and world objects (portals, traps, items, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorldInteractionService {

    private final PortalRepository portalRepository;

    /**
     * Checks if there is a portal at the specified coordinates.
     */
    public Optional<Portal> findPortalAt(UUID locationId, int x, int y) {
        return portalRepository.findBySourceLocationIdAndSourceXAndSourceY(locationId, x, y);
    }
    
    // Future expansion point: findTrapAt, findItemAt, etc.
}
