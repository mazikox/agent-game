package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing persistent Agent entities in Postgres.
 */
public interface AgentRepository extends JpaRepository<Agent, UUID> {
    List<Agent> findByStatus(AgentStatus status);
}
