package com.agentgierka.mmo.security;

import com.agentgierka.mmo.agent.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("agentSecurity")
@RequiredArgsConstructor
public class AgentSecurity {

    private final AgentRepository agentRepository;

    @Cacheable(value = "agentOwner", key = "#agentId + '_' + #currentUsername", unless = "#result == false")
    public boolean isOwner(UUID agentId, String currentUsername) {
        if (agentId == null || currentUsername == null) {
            return false;
        }
        
        return agentRepository.findById(agentId)
                .map(agent -> {
                    if (agent.getOwner() == null) return false;
                    return agent.getOwner().getUsername().equals(currentUsername);
                })
                .orElse(false);
    }
}
