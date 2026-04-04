package com.agentgierka.mmo.world.web.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PortalDto {
    private UUID id;
    private Integer sourceX;
    private Integer sourceY;
    private String targetLocationName;
}
