package com.agentgierka.mmo.ai.listener;

import com.agentgierka.mmo.ai.service.AgentThinkingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.agentgierka.mmo.ai.port.Brain;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class GoalAssignedListenerLoadTest {

    @Autowired
    private GoalAssignedListener goalAssignedListener;

    @MockitoBean
    private Brain brain;

    @MockitoBean
    private AgentWorldStateRepository agentWorldStateRepository;

    @Test
    void contextLoads() {
        assertNotNull(goalAssignedListener);
    }
}
