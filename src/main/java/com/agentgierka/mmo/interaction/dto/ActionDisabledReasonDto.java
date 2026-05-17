package com.agentgierka.mmo.interaction.dto;

import java.util.Map;

public record ActionDisabledReasonDto(
    String code,
    Map<String, Object> params
) {
    public static ActionDisabledReasonDto of(String code) {
        return new ActionDisabledReasonDto(code, Map.of());
    }

    public static ActionDisabledReasonDto of(String code, Map<String, Object> params) {
        return new ActionDisabledReasonDto(code, params);
    }
}
