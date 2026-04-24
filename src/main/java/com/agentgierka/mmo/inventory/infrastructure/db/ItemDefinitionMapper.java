package com.agentgierka.mmo.inventory.infrastructure.db;

import com.agentgierka.mmo.inventory.domain.ItemDefinition;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemDefinitionMapper {

    ItemDefinition toDomain(ItemDefinitionEntity entity);
}
