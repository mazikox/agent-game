package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.service.AgentService;
import com.agentgierka.mmo.agent.web.dto.*;
import com.agentgierka.mmo.agent.web.mapper.AgentMapper;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/agents")
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
    @PreAuthorize("@agentSecurity.isOwner(#id, authentication.name)")
    public AgentDto move(@PathVariable("id") UUID id, @Valid @RequestBody MoveRequest request) {
        var agent = agentService.moveTo(id, request.x(), request.y());
        log.info("Agent {} moving to ({}, {})", agent.getName(), request.x(), request.y());
        return agentMapper.toDto(agent);
    }

    @PostMapping("/{id}/goal")
    @PreAuthorize("@agentSecurity.isOwner(#id, authentication.name)")
    public AgentDto assignGoal(@PathVariable("id") UUID id, @Valid @RequestBody AssignGoalRequest request) {
        log.info("Assigning goal to agent {}: {}", id, request.goal());
        agentService.assignGoal(id, request.goal());
        
        // Fix L3: fetch the agent AFTER assigning goal to return fresh state
        var freshAgent = agentService.findById(id);
        return agentMapper.toDto(freshAgent);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("@agentSecurity.isOwner(#id, authentication.name)")
    public AgentDto updateStatus(@PathVariable("id") UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        log.info("Updating agent {} status to {}: {}", id, request.status(), request.description());
        return agentMapper.toDto(agentService.updateStatus(id, request.status(), request.description()));
    }

    @PostMapping("/{id}/interrupt")
    @PreAuthorize("@agentSecurity.isOwner(#id, authentication.name)")
    public AgentDto interrupt(@PathVariable("id") UUID id) {
        log.info("Interrupting agent {}", id);
        agentService.interruptAgent(id);
        return agentMapper.toDto(agentService.findById(id));
    }
}
