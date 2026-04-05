package com.agentgierka.mmo.security;

import com.agentgierka.mmo.agent.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("agentSecurity")
@RequiredArgsConstructor
public class AgentSecurity {

    private final AgentRepository agentRepository;

    public boolean isOwner(UUID agentId) {
        if (agentId == null || SecurityContextHolder.getContext().getAuthentication() == null) {
            return false;
        }
        
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        
        return agentRepository.findById(agentId)
                .map(agent -> {
                    if (agent.getOwner() == null) return false;
                    return agent.getOwner().getUsername().equals(currentUsername);
                })
                .orElse(false);
    }
}
