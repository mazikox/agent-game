package com.agentgierka.mmo.agent.web.mapper;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.web.dto.AgentDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AgentMapper {
    AgentMapper INSTANCE = Mappers.getMapper(AgentMapper.class);

    AgentDto toDto(Agent agent);

    Agent toEntity(AgentDto dto);
}
