package com.agentgierka.mmo.inventory.domain;

import java.util.Set;

public sealed interface InventoryResult permits
    InventoryResult.Success,
    InventoryResult.Collision,
    InventoryResult.OutOfBounds {

    record Success(int fromIndex, int toIndex) implements InventoryResult {}

    record Collision(int attemptedIndex, Set<Integer> collidingSlots) implements InventoryResult {}

    record OutOfBounds(int attemptedIndex, int itemWidth, int itemHeight) implements InventoryResult {}
}
