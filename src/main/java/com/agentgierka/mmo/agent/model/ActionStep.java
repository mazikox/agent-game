package com.agentgierka.mmo.agent.model;

import com.agentgierka.mmo.ai.model.ActionType;
import com.agentgierka.mmo.ai.model.QualifierType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ActionStep {

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private Integer targetIndex;

    @Enumerated(EnumType.STRING)
    private QualifierType qualifier;

    private Integer rawX;
    private Integer rawY;
    private String actionSummary;

    public static ActionStep create(ActionType actionType, Integer targetIndex, QualifierType qualifier, Integer rawX, Integer rawY, String actionSummary) {
        return ActionStep.builder()
                .actionType(actionType)
                .targetIndex(targetIndex)
                .qualifier(qualifier)
                .rawX(rawX)
                .rawY(rawY)
                .actionSummary(actionSummary)
                .build();
    }
}
