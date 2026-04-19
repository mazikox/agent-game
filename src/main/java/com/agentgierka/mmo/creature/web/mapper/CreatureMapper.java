package com.agentgierka.mmo.creature.web.mapper;

import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.web.dto.CreatureDto;
import org.mapstruct.Mapper;

/**
 * Mapper for converting CreatureInstance to CreatureDto.
 */
@Mapper(componentModel = "spring")
public interface CreatureMapper {
    CreatureDto toDto(CreatureInstance instance);
}
