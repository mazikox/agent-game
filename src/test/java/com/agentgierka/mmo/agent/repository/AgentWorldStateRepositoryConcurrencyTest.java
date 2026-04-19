package com.agentgierka.mmo.agent.repository;

import com.agentgierka.mmo.agent.model.AgentStatus;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the race condition in AgentWorldStateRepository.updateAtomic().
 * In the current (broken) implementation, both threads will succeed and
 * overwrite each other, whereas only one should succeed.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AgentWorldStateRepository Concurrency Proof")
@Disabled("Requires a running Redis instance on port 6380. Disabled for CI/isolated builds.")
class AgentWorldStateRepositoryConcurrencyTest {

    @Autowired
    private AgentWorldStateRepository repository;

    @Autowired
    private RedisTemplate<String, String> mmoStringRedisTemplate;

    private UUID agentId;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        repository.delete(agentId);
    }

    @Test
    @DisplayName("BUG PROOF: Concurrent updates should only allow one success")
    void concurrentUpdatesShouldOnlyAllowOneSuccess() throws InterruptedException {
        // 1. GIVEN: Agent exists with version 0
        AgentWorldState initialState = AgentWorldState.builder()
                .agentId(agentId)
                .agentName("RaceConditionAgent")
                .x(10).y(10)
                .version(0)
                .status(AgentStatus.MOVING)
                .build();
        repository.save(initialState);
        
        // DEBUG: Inspect Redis
        String raw = mmoStringRedisTemplate.opsForValue().get("agent:state:" + agentId);
        System.out.println("DEBUG RAW REDIS: [" + raw + "]");

        // 2. WHEN: Two threads attempt updateAtomic simultaneously with the same initial state (v0)
        int threadsCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadsCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    AgentWorldState stateToUpdate = repository.findById(agentId);
                    if (stateToUpdate == null) {
                        System.err.println("DEBUG: findById returned NULL for " + agentId);
                        return;
                    }
                    
                    latch.await();
                    
                    if (repository.updateAtomic(stateToUpdate)) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG THREAD EXCEPTION: " + e.getMessage());
                    e.printStackTrace(System.err);
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        latch.countDown();
        doneSignal.await();
        executor.shutdown();

        assertThat(successCount.get()).as("Exactly one update should succeed").isEqualTo(1);
        assertThat(failureCount.get()).as("Remaining updates should fail due to version mismatch").isEqualTo(threadsCount - 1);
    }
}
