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

import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AgentWorldStateRepositoryTest {

    @Mock
    private RedisTemplate<String, AgentWorldState> agentWorldStateTemplate;

    @Mock
    private RedisTemplate<String, String> mmoStringRedisTemplate;

    @Mock
    private ValueOperations<String, AgentWorldState> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    @SuppressWarnings("rawtypes")
    private RedisSerializer redisSerializer;

    private AgentWorldStateRepository repository;

    private UUID agentId;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        repository = new AgentWorldStateRepository(agentWorldStateTemplate, mmoStringRedisTemplate);
        
        lenient().when(agentWorldStateTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(mmoStringRedisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(agentWorldStateTemplate.getValueSerializer()).thenReturn(redisSerializer);
    }

    @Test
    void shouldFailToUpdateWhenVersionIsStale() {
        AgentWorldState staleStateFromEngine = AgentWorldState.builder()
                .agentId(agentId)
                .version(0L)
                .status(AgentStatus.MOVING)
                .build();

        when(redisSerializer.serialize(any())).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        
        // Mock result for stale version (0 returned by Lua)
        // Invocation has 6 args: script, keys, version, json, status, agentId
        doReturn(0L).when(mmoStringRedisTemplate).execute(any(RedisScript.class), anyList(), any(), any(), any(), any());

        boolean updateResult = repository.updateAtomic(staleStateFromEngine);

        assertThat(updateResult).isFalse();
        // Version should still be incremented because incrementVersion() is called before execute
        assertThat(staleStateFromEngine.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateSuccessWhenVersionIsCorrect() {
        AgentWorldState currentStateInRedis = AgentWorldState.builder()
                .agentId(agentId)
                .version(5L)
                .status(AgentStatus.MOVING)
                .build();
        
        when(redisSerializer.serialize(any())).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        
        // Mock result for success (1 returned by Lua)
        // Invocation has 6 args: script, keys, version, json, status, agentId
        doReturn(1L).when(mmoStringRedisTemplate).execute(any(RedisScript.class), anyList(), any(), any(), any(), any());

        boolean updateResult = repository.updateAtomic(currentStateInRedis);

        assertThat(updateResult).isTrue();
        assertThat(currentStateInRedis.getVersion()).isEqualTo(6L);
    }
}
