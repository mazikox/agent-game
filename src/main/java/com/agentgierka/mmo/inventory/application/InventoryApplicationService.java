package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.inventory.domain.InventoryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryTransactionService transactionService;

    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = "100ms"
    )
    public InventoryResult moveItem(UUID characterId, int fromIndex, int toIndex) {
        return transactionService.moveItem(characterId, fromIndex, toIndex);
    }
}
