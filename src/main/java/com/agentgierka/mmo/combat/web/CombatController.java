package com.agentgierka.mmo.combat.web;

import com.agentgierka.mmo.combat.model.CombatActionType;
import com.agentgierka.mmo.combat.model.CombatInstance;
import com.agentgierka.mmo.combat.service.CombatService;
import com.agentgierka.mmo.combat.web.dto.CombatResponse;
import com.agentgierka.mmo.combat.web.mapper.CombatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * REST Controller for combat interactions.
 */
@RestController
@RequestMapping("/api/combat")
@RequiredArgsConstructor
public class CombatController {

    private final CombatService combatService;
    private final CombatMapper combatMapper;

    /**
     * Starts a transition into combat for an agent and a creature.
     */
    @PostMapping("/initiate")
    @PreAuthorize("@agentSecurity.isOwner(#agentId, authentication.name)")
    public ResponseEntity<CombatResponse> initiateCombat(
            @RequestParam UUID agentId, 
            @RequestParam UUID creatureId) {
        CombatInstance combat = combatService.initiateCombat(agentId, creatureId);
        return ResponseEntity.ok(combatMapper.toResponse(combat));
    }

    /**
     * Performs a specific action in an ongoing combat.
     */
    @PostMapping("/action")
    @PreAuthorize("@agentSecurity.isOwner(#agentId, authentication.name)")
    public ResponseEntity<Void> executeAction(
            @RequestParam UUID agentId, 
            @RequestParam CombatActionType actionType) {
        combatService.executeAction(agentId, actionType);
        return ResponseEntity.ok().build();
    }
}
