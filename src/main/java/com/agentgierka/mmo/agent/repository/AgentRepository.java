package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing persistent Agent entities in Postgres.
 */
public interface AgentRepository extends JpaRepository<Agent, UUID> {
    List<Agent> findByStatus(AgentStatus status);
    List<Agent> findByCurrentLocationId(UUID locationId);
    List<Agent> findByOwnerUsername(String username);

    @Modifying
    @Query("UPDATE Agent a SET a.x = :x, a.y = :y WHERE a.id = :id")
    void updatePosition(@Param("id") UUID id, @Param("x") Integer x, @Param("y") Integer y);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Agent a WHERE a.id = :id")
    Optional<Agent> findByIdForUpdate(@Param("id") UUID id);
}
