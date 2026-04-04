package com.agentgierka.mmo.world.web.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Location details, including its map dimensions 
 * and available portals. Used by the frontend to render the world view.
 */
@Data
@Builder
public class LocationDto {
    private UUID id;
    private String name;
    private String description;
    private Integer width;
    private Integer height;
    private List<PortalDto> portals;
}
