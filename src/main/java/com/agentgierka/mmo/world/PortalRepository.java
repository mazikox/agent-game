package com.agentgierka.mmo.world;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    List<Portal> findAllBySourceLocationId(UUID sourceLocationId);

    @Query("SELECT p FROM Portal p JOIN FETCH p.targetLocation WHERE p.sourceLocation.id = :id")
    List<Portal> findAllBySourceLocationIdWithTarget(@Param("id") UUID id);
}

