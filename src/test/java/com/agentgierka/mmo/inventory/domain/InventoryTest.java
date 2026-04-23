package com.agentgierka.mmo.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryTest {

    private Inventory inventory;
    private ItemDefinition swordDef;
    private ItemDefinition potionDef;

    @BeforeEach
    void setUp() {
        // 5 columns x 9 rows
        inventory = new Inventory(5, 9);
        swordDef = new ItemDefinition("SWORD", "Sword", 1, 3, 1);
        potionDef = new ItemDefinition("POTION", "Potion", 1, 1, 20);
    }

    @Test
    @DisplayName("Should successfully move a 1x1 item to an empty slot")
    void shouldMoveSmallItem() {
        // given
        ItemStack potion = new ItemStack(UUID.randomUUID(), potionDef, 1);
        addItem(0, potion);

        // when
        InventoryResult result = inventory.processMove(0, 1);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems()).containsEntry(1, potion);
        assertThat(inventory.getAnchoredItems()).doesNotContainKey(0);
    }

    @Test
    @DisplayName("Should fail when moving item out of bounds (right edge wrap-around)")
    void shouldFailOutOfBoundsRight() {
        // given: 2x2 item at index 3 (slots 3,4,8,9) in 5-wide grid
        ItemDefinition bigItemDef = new ItemDefinition("BIG", "Big", 2, 2, 1);
        ItemStack bigItem = new ItemStack(UUID.randomUUID(), bigItemDef, 1);
        addItem(0, bigItem);

        // when: move to index 4 (would occupy 4, 5 <-- 5 is wrap around to next row)
        InventoryResult result = inventory.processMove(0, 4);

        // then
        assertThat(result).isInstanceOf(InventoryResult.OutOfBounds.class);
    }

    @Test
    @DisplayName("Should detect collision with another item's occupied fields")
    void shouldDetectCollision() {
        // given
        ItemStack sword = new ItemStack(UUID.randomUUID(), swordDef, 1);
        ItemStack potion = new ItemStack(UUID.randomUUID(), potionDef, 1);
        
        addItem(0, sword);  // occupies 0, 5, 10
        addItem(1, potion); // occupies 1

        // when: move potion to 5 (which is occupied by the middle of the sword)
        InventoryResult result = inventory.processMove(1, 5);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Collision.class);
        InventoryResult.Collision collision = (InventoryResult.Collision) result;
        assertThat(collision.collidingSlots()).contains(5);
    }

    @Test
    @DisplayName("Should allow moving item to its current position (no-op)")
    void shouldAllowMoveToSamePosition() {
        // given
        ItemStack potion = new ItemStack(UUID.randomUUID(), potionDef, 1);
        addItem(5, potion);

        // when
        InventoryResult result = inventory.processMove(5, 5);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems()).containsEntry(5, potion);
    }

    // Helper to bypass lack of 'addItem' method in current Inventory implementation
    // while maintaining occupiedSlots invariants.
    private void addItem(int index, ItemStack item) {
        // This simulates what a 'loot' or 'load' logic would do.
        // We'll refactor this once Inventory.java gets an addItem method.
        var field = reflectAnchoredItems();
        field.put(index, item);
        inventory.processMove(index, index); // Hack to trigger occupiedSlots calculation via atomic move logic
        // Wait, processMove(index, index) returns Success and does nothing.
        // Let's just manually fill it for now to keep it simple.
        calculateAndAddOccupied(index, item);
    }

    private void calculateAndAddOccupied(int index, ItemStack item) {
        int col = index % 5;
        int row = index / 5;
        for (int r = 0; r < item.getHeight(); r++) {
            for (int c = 0; c < item.getWidth(); c++) {
                reflectOccupiedSlots().add((row + r) * 5 + (col + c));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, ItemStack> reflectAnchoredItems() {
        try {
            var f = Inventory.class.getDeclaredField("anchoredItems");
            f.setAccessible(true);
            return (java.util.Map<Integer, ItemStack>) f.get(inventory);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    private java.util.Set<Integer> reflectOccupiedSlots() {
        try {
            var f = Inventory.class.getDeclaredField("occupiedSlots");
            f.setAccessible(true);
            return (java.util.Set<Integer>) f.get(inventory);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
