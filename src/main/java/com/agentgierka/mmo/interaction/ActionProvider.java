package com.agentgierka.mmo.interaction;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.interaction.dto.ActionDescriptorDto;
import java.util.List;
import java.util.UUID;

public interface ActionProvider {
    TargetType getSupportedTargetType();
    List<ActionDescriptorDto> getActions(UUID targetId, Agent agent);
}
