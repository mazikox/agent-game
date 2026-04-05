package com.agentgierka.mmo.agent.web.mapper;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.web.dto.AgentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AgentMapper {

    @Mapping(target = "currentLocationId", source = "currentLocation.id")
    @Mapping(target = "hp", source = "stats.hp")
    @Mapping(target = "maxHp", source = "stats.maxHp")
    @Mapping(target = "experience", source = "stats.experience")
    @Mapping(target = "expThreshold", source = "stats.expThreshold")
    @Mapping(target = "level", source = "stats.level")
    AgentDto toDto(Agent agent);
}
