package com.agentgierka.mmo.combat.web.mapper;

import com.agentgierka.mmo.combat.model.CombatInstance;
import com.agentgierka.mmo.combat.web.dto.CombatResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CombatMapper {

    @Mapping(source = "id", target = "combatId")
    CombatResponse toResponse(CombatInstance combat);
}
