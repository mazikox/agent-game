package com.agentgierka.mmo.world;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Portal entities.
 */
public interface PortalRepository extends JpaRepository<Portal, UUID> {
    
    /**
     * Finds a portal based on the source location and coordinates.
     */
    Optional<Portal> findBySourceLocationAndSourceXAndSourceY(Location sourceLocation, Integer sourceX, Integer sourceY);

    java.util.List<Portal> findAllBySourceLocation(Location sourceLocation);
}

