package com.agentgierka.mmo.interaction;

import com.agentgierka.mmo.interaction.dto.InteractionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @GetMapping
    public InteractionResponse getAvailableActions(
        @RequestParam UUID agentId,
        @RequestParam UUID targetId,
        @RequestParam TargetType targetType,
        @RequestParam(required = false, defaultValue = "") String targetName
    ) {
        return interactionService.getInteractions(agentId, targetId, targetType, targetName);
    }
}
