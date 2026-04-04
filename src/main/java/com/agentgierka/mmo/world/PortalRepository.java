package com.agentgierka.mmo.world;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Portal entities.
 */
public interface PortalRepository extends JpaRepository<Portal, UUID> {
    
    /**
     * Finds a portal based on the source location ID and coordinates.
     */
    Optional<Portal> findBySourceLocationIdAndSourceXAndSourceY(UUID sourceLocationId, Integer sourceX, Integer sourceY);

    java.util.List<Portal> findAllBySourceLocationId(UUID sourceLocationId);
}

