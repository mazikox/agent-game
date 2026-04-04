package com.agentgierka.mmo.world.web.mapper;

import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.web.dto.LocationDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Location entities to DTOs.
 * Following pure DDD/Hexagonal principles by isolating from other domains.
 */
@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationDto toDto(Location location);
}
