package com.agentgierka.mmo.interaction;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.interaction.dto.InteractionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    private final AgentRepository agentRepository;
    private final Map<TargetType, ActionProvider> providerMap;

    public InteractionService(AgentRepository agentRepository, List<ActionProvider> providers) {
        this.agentRepository = agentRepository;
        this.providerMap = providers.stream()
            .collect(Collectors.toMap(
                ActionProvider::getSupportedTargetType,
                Function.identity()
            ));
    }

    @Transactional(readOnly = true)
    public InteractionResponse getInteractions(UUID agentId, UUID targetId, TargetType targetType, String targetName) {
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new AgentNotFoundException(agentId.toString()));

        ActionProvider provider = getProvider(targetType);

        return new InteractionResponse(
            targetId,
            targetType.name(),
            targetName,
            provider.getActions(targetId, agent)
        );
    }

    private ActionProvider getProvider(TargetType targetType) {
        ActionProvider provider = providerMap.get(targetType);
        if (provider == null) {
            throw new IllegalArgumentException("No provider for target type: " + targetType);
        }
        return provider;
    }
}
