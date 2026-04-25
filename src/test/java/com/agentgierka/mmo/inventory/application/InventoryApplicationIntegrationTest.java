package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.inventory.domain.InventoryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.EnableResilientMethods;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.data.redis.connection.RedisConnectionFactory;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InventoryApplicationIntegrationTest.RetryTestConfig.class)
class InventoryApplicationIntegrationTest {

    @Autowired
    private InventoryApplicationService applicationService;

    @Autowired
    private InventoryTransactionService transactionService;

    @TestConfiguration
    @EnableResilientMethods
    static class RetryTestConfig {
        @Bean
        public InventoryTransactionService inventoryTransactionService() {
            return mock(InventoryTransactionService.class);
        }

        @Bean
        public InventoryApplicationService inventoryApplicationService(InventoryTransactionService transactionService) {
            return new InventoryApplicationService(transactionService);
        }

    }

    @Test
    @DisplayName("Should retry operation when OptimisticLockingFailureException occurs")
    void shouldRetryOnConflict() {
        // Given
        UUID characterId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        when(transactionService.moveItem(any(), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                if (callCount.incrementAndGet() < 3) {
                    throw new OptimisticLockingFailureException("Conflict");
                }
                return new InventoryOperationResult(new InventoryResult.Success(0, 1), null);
            });

        // When
        InventoryOperationResult opResult = applicationService.moveItem(characterId, 0, 1);

        // Then
        assertThat(opResult.result()).isInstanceOf(InventoryResult.Success.class);
        assertThat(callCount.get()).isEqualTo(3);
        verify(transactionService, times(3)).moveItem(any(), anyInt(), anyInt());
    }
}
