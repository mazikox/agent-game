package com.agentgierka.mmo.interaction.provider;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.interaction.ActionProvider;
import com.agentgierka.mmo.interaction.TargetType;
import com.agentgierka.mmo.interaction.dto.ActionDescriptorDto;
import com.agentgierka.mmo.interaction.dto.ActionDisabledReasonDto;
import com.agentgierka.mmo.world.Portal;
import com.agentgierka.mmo.world.PortalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PortalActionProvider implements ActionProvider {

    private static final int PORTAL_RANGE = 15;
    private final PortalRepository portalRepository;

    @Override
    public TargetType getSupportedTargetType() {
        return TargetType.PORTAL;
    }

    @Override
    public List<ActionDescriptorDto> getActions(UUID targetId, Agent agent) {
        Portal portal = portalRepository.findById(targetId)
            .orElseThrow(() -> new RuntimeException("Portal not found: " + targetId));

        List<ActionDescriptorDto> actions = new ArrayList<>();
        double distance = calculateDistance(agent, portal);
        boolean agentInCombat = agent.getStatus() == AgentStatus.IN_COMBAT;
        boolean inRange = distance <= PORTAL_RANGE;

        actions.add(new ActionDescriptorDto(
            "examine",
            "eye",
            null,
            null,
            true,
            null
        ));

        actions.add(new ActionDescriptorDto(
            "enter_portal",
            "door-enter",
            "/api/v1/agents/{agentId}/portal/enter",
            "POST",
            inRange && !agentInCombat,
            getEnterPortalDisabledReason(inRange, agentInCombat, distance)
        ));

        return actions;
    }

    private ActionDisabledReasonDto getEnterPortalDisabledReason(boolean inRange, boolean agentInCombat, double distance) {
        if (!inRange) {
            return ActionDisabledReasonDto.of("TOO_FAR", Map.of(
                "current", (int) distance,
                "max", PORTAL_RANGE
            ));
        }
        if (agentInCombat) {
            return ActionDisabledReasonDto.of("AGENT_IN_COMBAT");
        }
        return null;
    }

    private double calculateDistance(Agent agent, Portal portal) {
        int agentX = agent.getX() != null ? agent.getX() : 0;
        int agentY = agent.getY() != null ? agent.getY() : 0;
        int dx = agentX - portal.getSourceX();
        int dy = agentY - portal.getSourceY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
