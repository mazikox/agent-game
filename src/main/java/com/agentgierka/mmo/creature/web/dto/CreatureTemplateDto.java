package com.agentgierka.mmo.creature.web.dto;

import com.agentgierka.mmo.creature.model.CreatureRank;
import java.util.UUID;

public record CreatureTemplateDto(
        UUID id,
        String name,
        CreatureRank rank,
        int level,
        String iconUrl
) {}
