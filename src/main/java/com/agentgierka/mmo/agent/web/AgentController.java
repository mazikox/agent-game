package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.service.AgentService;
import com.agentgierka.mmo.agent.web.dto.AgentDto;
import com.agentgierka.mmo.agent.web.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Master-to-Agent interactions.
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final AgentMapper agentMapper;

    /**
     * Lists all agents in the world.
     */
    @GetMapping
    public List<AgentDto> listAll() {
        return agentService.findAll().stream()
                .map(agentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single agent's state (Hybrid: Redis if moving, Postgres otherwise).
     */
    @GetMapping("/{id}")
    public AgentDto get(@PathVariable UUID id) {
        return agentMapper.toDto(agentService.findById(id));
    }

    /**
     * Requests the agent to move to a specific location.
     */
    @PostMapping("/{id}/move")
    public AgentDto move(@PathVariable UUID id, @RequestParam Integer x, @RequestParam Integer y) {
        return agentMapper.toDto(agentService.moveTo(id, x, y));
    }

    /**
     * Assigns a high-level goal to the agent, triggering the AI thought process.
     */
    @PostMapping("/{id}/goal")
    public AgentDto assignGoal(@PathVariable UUID id, @RequestParam String goal) {
        log.info("--- Incoming Goal Request for Agent {}: '{}' ---", id, goal);
        agentService.assignGoal(id, goal);
        return agentMapper.toDto(agentService.findById(id));
    }

    /**
     * Manually updates the agent's operational status.
     */
    @PatchMapping("/{id}/status")
    public AgentDto updateStatus(@PathVariable UUID id, @RequestParam AgentStatus status, @RequestParam String description) {
        return agentMapper.toDto(agentService.updateStatus(id, status, description));
    }
}
