package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.service.AgentService;
import com.agentgierka.mmo.agent.web.dto.AgentDto;
import com.agentgierka.mmo.agent.web.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@agentSecurity.isOwner(#id)")
    public AgentDto move(@PathVariable("id") UUID id, @RequestParam("x") Integer x, @RequestParam("y") Integer y) {
        var agent = agentService.findById(id);
        log.info("Agent {} moving to ({}, {})", agent.getName(), x, y);
        return agentMapper.toDto(agentService.moveTo(id, x, y));
    }

    @PostMapping("/{id}/goal")
    @PreAuthorize("@agentSecurity.isOwner(#id)")
    public AgentDto assignGoal(@PathVariable("id") UUID id, @RequestBody String goal) {
        var agent = agentService.findById(id);
        log.info("Assigning goal to agent {}: {}", agent.getName(), goal);
        agentService.assignGoal(id, goal);
        return agentMapper.toDto(agent);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("@agentSecurity.isOwner(#id)")
    public AgentDto updateStatus(@PathVariable("id") UUID id, @RequestParam("status") String status, @RequestParam("description") String description) {
        log.info("Updating agent {} status to {}: {}", id, status, description);
        AgentStatus agentStatus = AgentStatus.valueOf(status.toUpperCase());
        return agentMapper.toDto(agentService.updateStatus(id, agentStatus, description));
    }

    @PostMapping("/{id}/interrupt")
    @PreAuthorize("@agentSecurity.isOwner(#id)")
    public AgentDto interrupt(@PathVariable("id") UUID id) {
        log.info("Interrupting agent {}", id);
        agentService.interruptAgent(id);
        return agentMapper.toDto(agentService.findById(id));
    }
}
