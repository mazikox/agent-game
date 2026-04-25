package com.agentgierka.mmo.combat;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.combat.model.CombatActionType;
import com.agentgierka.mmo.combat.model.CombatStatus;
import com.agentgierka.mmo.combat.repository.CombatRepository;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureState;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.agentgierka.mmo.agent.service.WorldStateSynchronizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Combat Integration Test: From Encounter to Victory")
class CombatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CombatRepository combatRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private CreatureInstanceRepository creatureInstanceRepository;

    @MockitoBean
    private WorldStateSynchronizer worldStateSynchronizer;

    private UUID agentId;
    private final UUID creatureId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        transactionTemplate.executeWithoutResult(s -> {
            combatRepository.deleteAll();
            agentRepository.deleteAll();
            locationRepository.deleteAll();
            playerRepository.deleteAll();
        });

        // Setup World
        Location forest = transactionTemplate.execute(s -> locationRepository.save(
                Location.builder().name("Test Forest").width(100).height(100).type(LocationType.FOREST).build()
        ));

        Player player = transactionTemplate.execute(s -> playerRepository.save(
                Player.create("CombatPlayer", "secret")
        ));

        Agent agent = transactionTemplate.execute(s -> agentRepository.save(
                Agent.create("Warrior", player, forest, 0, 0, 1)
        ));
        agentId = agent.getId();

        // Mock Creature (100 HP, 10 AttkSpeed, 10 Damage)
        CreatureInstance creature = CreatureInstance.builder()
                .instanceId(creatureId)
                .name("Slime")
                .currentHp(20) // Low HP for quick test
                .maxHp(20)
                .attackSpeed(10)
                .damage(2)
                .experienceReward(50)
                .state(CreatureState.ALIVE)
                .build();

        when(creatureInstanceRepository.findById(creatureId)).thenReturn(creature);
        // Mock atomic update success
        when(creatureInstanceRepository.updateAtomic(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Should simulate full combat flow: Initiate -> Attack -> Victory")
    void shouldRunFullCombatFlow() throws Exception {
        // 1. INITIATE COMBAT
        mockMvc.perform(post("/api/combat/initiate")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                .param("agentId", agentId.toString())
                .param("creatureId", creatureId.toString()))
                .andExpect(status().isOk());

        // Verify status changed in DB
        Agent agentInCombat = agentRepository.findById(agentId).orElseThrow();
        assertThat(agentInCombat.getStatus()).isEqualTo(AgentStatus.IN_COMBAT);

        // 2. PERFORM ATTACK
        // Warrior has 15 baseDamage. Slime has 20 HP. Should take 2 hits.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/combat/action")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                    .param("agentId", agentId.toString())
                    .param("actionType", CombatActionType.ATTACK.toString()))
                    .andExpect(status().isOk());
        }

        // 3. VERIFY VICTORY
        // After 4 hits, creature should be dead, combat finished
        transactionTemplate.executeWithoutResult(s -> {
            Agent victoriousAgent = agentRepository.findById(agentId).orElseThrow();
            assertThat(victoriousAgent.getStatus()).isEqualTo(AgentStatus.IDLE);
            assertThat(victoriousAgent.getStats().getExperience()).isEqualTo(50);
            
            assertThat(combatRepository.findAll()).hasSize(1);
            assertThat(combatRepository.findAll().get(0).getStatus()).isEqualTo(CombatStatus.FINISHED);
        });
    }

    @Test
    @DisplayName("Should return 409 Conflict when initiating combat while already in combat")
    void shouldReturn409WhenAlreadyInCombat() throws Exception {
        // 1. Initiate first combat
        mockMvc.perform(post("/api/combat/initiate")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                .param("agentId", agentId.toString())
                .param("creatureId", creatureId.toString()))
                .andExpect(status().isOk());

        // 2. Try initiating again
        mockMvc.perform(post("/api/combat/initiate")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                .param("agentId", agentId.toString())
                .param("creatureId", creatureId.toString()))
                .andExpect(status().isConflict()); // Maped from AgentStateException
    }

    @Test
    @DisplayName("Should return 404 Not Found when creature is missing")
    void shouldReturn404WhenCreatureMissing() throws Exception {
        UUID nonExistentCreatureId = UUID.randomUUID();
        when(creatureInstanceRepository.findById(nonExistentCreatureId)).thenReturn(null);

        mockMvc.perform(post("/api/combat/initiate")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                .param("agentId", agentId.toString())
                .param("creatureId", nonExistentCreatureId.toString()))
                .andExpect(status().isNotFound()); // Maped from CreatureNotFoundException
    }

    @Test
    @DisplayName("Should return 400 Bad Request when it is not agent's turn")
    void shouldReturn400WhenWaitForTurn() throws Exception {
        // Initial setup to make creature faster
        transactionTemplate.executeWithoutResult(s -> {
            Agent agent = agentRepository.findById(agentId).orElseThrow();
            agent.getStats().setAttackSpeed(1); // Super slow
            agentRepository.save(agent);
        });

        // Mock creature with very high speed
        CreatureInstance fastCreature = CreatureInstance.builder()
                .instanceId(creatureId)
                .name("FastSlime")
                .currentHp(20)
                .maxHp(20)
                .attackSpeed(200) // Super fast
                .damage(2)
                .state(CreatureState.ALIVE)
                .build();
        when(creatureInstanceRepository.findById(creatureId)).thenReturn(fastCreature);

        // Initiate
        mockMvc.perform(post("/api/combat/initiate")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                .param("agentId", agentId.toString())
                .param("creatureId", creatureId.toString()))
                .andExpect(status().isOk());

        // Try attack - should fail because creature has turn
        mockMvc.perform(post("/api/combat/action")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("CombatPlayer"))
                .param("agentId", agentId.toString())
                .param("actionType", CombatActionType.ATTACK.toString()))
                .andExpect(status().isBadRequest()); // Maped from CombatException
    }
}
