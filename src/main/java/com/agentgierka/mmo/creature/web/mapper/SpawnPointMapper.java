package com.agentgierka.mmo.creature.web.mapper;

import com.agentgierka.mmo.creature.model.CreatureTemplate;
import com.agentgierka.mmo.creature.model.SpawnPoint;
import com.agentgierka.mmo.creature.web.dto.CreatureTemplateDto;
import com.agentgierka.mmo.creature.web.dto.SpawnPointDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpawnPointMapper {

    CreatureTemplateDto toDto(CreatureTemplate template);

    @Mapping(target = "templateId", source = "creatureTemplate.id")
    @Mapping(target = "templateName", source = "creatureTemplate.name")
    @Mapping(target = "locationId", source = "location.id")
    SpawnPointDto toDto(SpawnPoint point);
}
