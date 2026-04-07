package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentWorldStateRepositoryTest {

    @Mock
    private RedisTemplate<String, AgentWorldState> agentWorldStateTemplate;

    @Mock
    private RedisTemplate<String, String> mmoStringRedisTemplate;

    @Mock
    private ValueOperations<String, AgentWorldState> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private AgentWorldStateRepository repository;

    private UUID agentId;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        // Manual initialization avoids Mockito injection ambiguity with multiple RedisTemplates
        repository = new AgentWorldStateRepository(agentWorldStateTemplate, mmoStringRedisTemplate);
        
        lenient().when(agentWorldStateTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(mmoStringRedisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void shouldFailToUpdateWhenVersionIsStale() {
        AgentWorldState currentStateInRedis = AgentWorldState.builder()
                .agentId(agentId)
                .version(1L)
                .status(AgentStatus.MOVING)
                .build();
        
        AgentWorldState staleStateFromEngine = AgentWorldState.builder()
                .agentId(agentId)
                .version(0L)
                .status(AgentStatus.MOVING)
                .build();

        when(valueOperations.get(anyString())).thenReturn(currentStateInRedis);

        boolean updateResult = repository.updateAtomic(staleStateFromEngine);

        assertThat(updateResult).isFalse();
        verify(valueOperations, never()).set(anyString(), any());
    }

    @Test
    void shouldUpdateSuccessWhenVersionIsCorrect() {
        AgentWorldState currentStateInRedis = AgentWorldState.builder()
                .agentId(agentId)
                .version(5L)
                .status(AgentStatus.MOVING)
                .build();
        
        when(valueOperations.get(anyString())).thenReturn(currentStateInRedis);

        boolean updateResult = repository.updateAtomic(currentStateInRedis);

        assertThat(updateResult).isTrue();
        assertThat(currentStateInRedis.getVersion()).isEqualTo(6L);
        verify(valueOperations).set(anyString(), eq(currentStateInRedis));
    }
}
