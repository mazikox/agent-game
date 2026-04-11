package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing persistent Agent entities in Postgres.
 */
public interface AgentRepository extends JpaRepository<Agent, UUID> {
    List<Agent> findByStatus(AgentStatus status);

    @Modifying
    @Query("UPDATE Agent a SET a.x = :x, a.y = :y WHERE a.id = :id")
    void updatePosition(@Param("id") UUID id, @Param("x") Integer x, @Param("y") Integer y);
}
