package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Master-to-Agent interactions.
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * Lists all agents in the world.
     */
    @GetMapping
    public java.util.List<Agent> listAll() {
        return agentService.findAll();
    }

    /**
     * Gets a single agent's state (Hybrid: Redis if moving, Postgres otherwise).
     */
    @GetMapping("/{id}")
    public Agent get(@PathVariable UUID id) {
        return agentService.findById(id);
    }

    /**
     * Requests the agent to move to a specific location.
     */
    @PostMapping("/{id}/move")
    public Agent move(@PathVariable UUID id, @RequestParam Integer x, @RequestParam Integer y) {
        return agentService.moveTo(id, x, y);
    }

    /**
     * Manually updates the agent's operational status.
     */
    @PatchMapping("/{id}/status")
    public Agent updateStatus(@PathVariable UUID id, @RequestParam AgentStatus status, @RequestParam String description) {
        return agentService.updateStatus(id, status, description);
    }
}
