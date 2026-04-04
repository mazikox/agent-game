package com.agentgierka.mmo.agent.web.mapper;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.web.dto.AgentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AgentMapper {

    @Mapping(target = "currentLocationId", source = "currentLocation.id")
    AgentDto toDto(Agent agent);
}
