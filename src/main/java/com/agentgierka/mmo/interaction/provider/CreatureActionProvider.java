package com.agentgierka.mmo.interaction.provider;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.combat.model.CombatStatus;
import com.agentgierka.mmo.combat.repository.CombatRepository;
import com.agentgierka.mmo.creature.exception.CreatureNotFoundException;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.interaction.ActionProvider;
import com.agentgierka.mmo.interaction.TargetType;
import com.agentgierka.mmo.interaction.dto.ActionDescriptorDto;
import com.agentgierka.mmo.interaction.dto.ActionDisabledReasonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreatureActionProvider implements ActionProvider {

    private static final int ATTACK_RANGE = 30;
    private static final int APPROACH_RANGE = 5;

    private final CreatureInstanceRepository creatureRepository;
    private final CombatRepository combatRepository;

    @Override
    public TargetType getSupportedTargetType() {
        return TargetType.CREATURE;
    }

    @Override
    public List<ActionDescriptorDto> getActions(UUID targetId, Agent agent) {
        CreatureInstance creature = creatureRepository.findById(targetId);
        if (creature == null) {
            throw new CreatureNotFoundException(targetId.toString());
        }

        List<ActionDescriptorDto> actions = new ArrayList<>();
        double distance = calculateDistance(agent, creature);
        boolean agentInCombat = agent.getStatus() == AgentStatus.IN_COMBAT;
        boolean creatureAlive = creature.isAlive();
        boolean creatureInCombat = combatRepository
            .findByCreatureInstanceIdAndStatus(targetId, CombatStatus.ONGOING)
            .isPresent();

        actions.add(new ActionDescriptorDto(
            "examine",
            "eye",
            null,
            null,
            true,
            null
        ));

        boolean tooClose = distance <= APPROACH_RANGE;
        boolean approachEnabled = !agentInCombat && creatureAlive && !tooClose;
        ActionDisabledReasonDto approachReason = approachEnabled ? null : getApproachDisabledReason(agentInCombat, creatureAlive, tooClose);
        actions.add(new ActionDescriptorDto(
            "approach",
            "move",
            "/api/v1/agents/{agentId}/move",
            "POST",
            approachEnabled,
            approachReason
        ));

        boolean inRange = distance <= ATTACK_RANGE;
        boolean attackEnabled = creatureAlive && !agentInCombat && !creatureInCombat && inRange;
        ActionDisabledReasonDto attackReason = attackEnabled ? null : getAttackDisabledReason(creatureAlive, agentInCombat, creatureInCombat, inRange, distance);
        actions.add(new ActionDescriptorDto(
            "attack",
            "sword",
            "/api/combat/initiate",
            "POST",
            attackEnabled,
            attackReason
        ));

        return actions;
    }

    private ActionDisabledReasonDto getApproachDisabledReason(boolean agentInCombat, boolean creatureAlive, boolean tooClose) {
        if (agentInCombat) return ActionDisabledReasonDto.of("AGENT_IN_COMBAT");
        if (!creatureAlive) return ActionDisabledReasonDto.of("TARGET_DEAD");
        if (tooClose) return ActionDisabledReasonDto.of("ALREADY_CLOSE");
        return null;
    }

    private ActionDisabledReasonDto getAttackDisabledReason(boolean creatureAlive, boolean agentInCombat, boolean creatureInCombat, boolean inRange, double distance) {
        if (!creatureAlive) return ActionDisabledReasonDto.of("TARGET_DEAD");
        if (agentInCombat) return ActionDisabledReasonDto.of("AGENT_IN_COMBAT");
        if (creatureInCombat) return ActionDisabledReasonDto.of("CREATURE_IN_COMBAT");
        if (!inRange) {
            return ActionDisabledReasonDto.of("TOO_FAR", Map.of(
                "current", (int) distance,
                "max", ATTACK_RANGE
            ));
        }
        return null;
    }

    private double calculateDistance(Agent agent, CreatureInstance creature) {
        int agentX = agent.getX() != null ? agent.getX() : 0;
        int agentY = agent.getY() != null ? agent.getY() : 0;
        int dx = agentX - creature.getX();
        int dy = agentY - creature.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
