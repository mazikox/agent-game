package com.agentgierka.mmo.creature;

import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.model.CreatureRank;
import com.agentgierka.mmo.creature.model.CreatureState;
import com.agentgierka.mmo.creature.repository.CreatureInstanceRepository;
import com.agentgierka.mmo.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class CreatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatureInstanceRepository creatureInstanceRepository;

    @Test
    @WithMockUser
    void shouldReturnAliveCreaturesInLocation() throws Exception {
        UUID locationId = UUID.randomUUID();
        UUID instance1 = UUID.randomUUID();
        UUID instance2 = UUID.randomUUID();

        CreatureInstance alive1 = CreatureInstance.builder()
                .instanceId(instance1)
                .name("Wolf")
                .rank(CreatureRank.NORMAL)
                .state(CreatureState.ALIVE)
                .x(10).y(20).level(1).currentHp(50).maxHp(50)
                .build();

        CreatureInstance dead1 = CreatureInstance.builder()
                .instanceId(instance2)
                .name("Dead Spider")
                .state(CreatureState.DEAD)
                .build();

        Mockito.when(creatureInstanceRepository.findAllByLocationId(locationId))
                .thenReturn(List.of(alive1, dead1));

        mockMvc.perform(get("/api/v1/locations/" + locationId + "/creatures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].instanceId").value(instance1.toString()))
                .andExpect(jsonPath("$[0].name").value("Wolf"))
                .andExpect(jsonPath("$[0].level").value(1))
                .andExpect(jsonPath("$[0].x").value(10))
                .andExpect(jsonPath("$[0].state").value("ALIVE"));
    }
}
