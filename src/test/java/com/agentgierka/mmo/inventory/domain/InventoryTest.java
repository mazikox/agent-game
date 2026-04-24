package com.agentgierka.mmo.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
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
        inventory.placeItem(0, potion);

        // when
        InventoryResult result = inventory.processMove(0, 1);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems()).containsEntry(1, potion);
        assertThat(inventory.getAnchoredItems()).doesNotContainKey(0);
    }

    @Test
    @DisplayName("Should swap two 1x1 items positions")
    void shouldSwapTwoSmallItems() {
        // given
        ItemDefinition ringDef = new ItemDefinition("RING", "Ring", 1, 1, 1);
        ItemStack ringA = new ItemStack(UUID.randomUUID(), ringDef, 1);
        ItemStack ringB = new ItemStack(UUID.randomUUID(), ringDef, 1);
        inventory.placeItem(0, ringA);
        inventory.placeItem(1, ringB);

        // when
        InventoryResult result = inventory.processMove(0, 1);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems().get(1)).isSameAs(ringA);
        assertThat(inventory.getAnchoredItems().get(0)).isSameAs(ringB);
    }

    @Test
    @DisplayName("Should swap big item (2x3) with small item (1x1)")
    void shouldSwapBigWithSmall() {
        // given
        ItemDefinition armorDef = new ItemDefinition("ARMOR", "Armor", 2, 3, 1);
        ItemStack armor = new ItemStack(UUID.randomUUID(), armorDef, 1);
        ItemStack potion = new ItemStack(UUID.randomUUID(), potionDef, 1);
        
        inventory.placeItem(0, armor);   
        inventory.placeItem(14, potion); 

        // when: Move armor to 13.
        InventoryResult result = inventory.processMove(0, 13);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems().get(13)).isSameAs(armor);
        assertThat(inventory.getAnchoredItems().get(0)).isSameAs(potion);
    }

    @Test
    @DisplayName("Should fail swap when overlapping with multiple items")
    void shouldFailSwapWithMultipleItems() {
        // given
        ItemDefinition armorDef = new ItemDefinition("ARMOR", "Armor", 2, 2, 1);
        ItemStack armor = new ItemStack(UUID.randomUUID(), armorDef, 1);
        ItemStack potion1 = new ItemStack(UUID.randomUUID(), potionDef, 1);
        ItemStack potion2 = new ItemStack(UUID.randomUUID(), potionDef, 1);
        
        inventory.placeItem(0, armor);
        inventory.placeItem(10, potion1);
        inventory.placeItem(11, potion2);

        // when: Move armor to 10. Overlaps with both potion1(10) and potion2(11)
        InventoryResult result = inventory.processMove(0, 10);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Collision.class);
    }

    @Test
    @DisplayName("Should merge stackable items of same type")
    void shouldMergeStacks() {
        // given
        ItemStack potion1 = new ItemStack(UUID.randomUUID(), potionDef, 5);
        ItemStack potion2 = new ItemStack(UUID.randomUUID(), potionDef, 10);
        inventory.placeItem(0, potion1);
        inventory.placeItem(1, potion2);

        // when
        InventoryResult result = inventory.processMove(0, 1);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems().get(1).getQuantity()).isEqualTo(15);
        assertThat(inventory.getAnchoredItems()).doesNotContainKey(0);
    }

    @Test
    @DisplayName("Should partial merge when exceeding max stack size")
    void shouldPartialMerge() {
        // given
        ItemStack potion1 = new ItemStack(UUID.randomUUID(), potionDef, 15);
        ItemStack potion2 = new ItemStack(UUID.randomUUID(), potionDef, 10); // max is 20
        inventory.placeItem(0, potion1);
        inventory.placeItem(1, potion2);

        // when
        InventoryResult result = inventory.processMove(0, 1);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems().get(1).getQuantity()).isEqualTo(20);
        assertThat(inventory.getAnchoredItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should fail when moving item out of bounds")
    void shouldFailOutOfBounds() {
        // given
        ItemDefinition bigDef = new ItemDefinition("BIG", "Big", 2, 2, 1);
        ItemStack bigItem = new ItemStack(UUID.randomUUID(), bigDef, 1);
        inventory.placeItem(0, bigItem);

        // when: Moving 2x2 to index 4 (right edge)
        InventoryResult result = inventory.processMove(0, 4);

        // then
        assertThat(result).isInstanceOf(InventoryResult.OutOfBounds.class);
    }

    @Test
    @DisplayName("Should find first empty slot for 1x1 item")
    void shouldFindEmptySlot() {
        // given
        ItemDefinition ringDef = new ItemDefinition("RING", "Ring", 1, 1, 1);
        ItemStack ring1 = new ItemStack(UUID.randomUUID(), ringDef, 1);
        ItemStack ring2 = new ItemStack(UUID.randomUUID(), ringDef, 1);
        inventory.placeItem(0, ring1);

        // when
        InventoryResult result = inventory.addItem(ring2);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems().get(1)).isSameAs(ring2);
    }

    @Test
    @DisplayName("Should find space for 1x3 sword when first slots are occupied")
    void shouldFindSpaceForBigItem() {
        // given
        ItemDefinition ringDef = new ItemDefinition("RING", "Ring", 1, 1, 1);
        inventory.placeItem(0, new ItemStack(UUID.randomUUID(), ringDef, 1));
        inventory.placeItem(1, new ItemStack(UUID.randomUUID(), ringDef, 1));
        
        ItemStack sword = new ItemStack(UUID.randomUUID(), swordDef, 1);

        // when
        InventoryResult result = inventory.addItem(sword);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems().get(2)).isSameAs(sword);
    }

    @Test
    @DisplayName("Should automatically stack item when picking up")
    void shouldAutoStackOnPickup() {
        // given
        ItemStack existingPotion = new ItemStack(UUID.randomUUID(), potionDef, 5);
        inventory.placeItem(10, existingPotion);
        
        ItemStack newPotion = new ItemStack(UUID.randomUUID(), potionDef, 5);

        // when
        InventoryResult result = inventory.addItem(newPotion);

        // then
        assertThat(result).isInstanceOf(InventoryResult.Success.class);
        assertThat(existingPotion.getQuantity()).isEqualTo(10);
        assertThat(inventory.getAnchoredItems().values()).hasSize(1);
    }

    @Test
    @DisplayName("Should fail to add item when no space is available")
    void shouldFailWhenFull() {
        // given
        ItemDefinition ringDef = new ItemDefinition("RING", "Ring", 1, 1, 1);
        Inventory tinyInventory = new Inventory(1, 1);
        tinyInventory.placeItem(0, new ItemStack(UUID.randomUUID(), ringDef, 1));
        
        ItemStack anotherItem = new ItemStack(UUID.randomUUID(), ringDef, 1);

        // when
        InventoryResult result = tinyInventory.addItem(anotherItem);

        // then
        assertThat(result).isInstanceOf(InventoryResult.NoSpace.class);
    }

    @Test
    @DisplayName("Should successfully remove item and free up slots")
    void shouldRemoveItem() {
        // given
        ItemStack potion = new ItemStack(UUID.randomUUID(), potionDef, 1);
        inventory.placeItem(0, potion);
        assertThat(inventory.getAnchoredItems()).containsKey(0);

        // when
        Optional<ItemStack> removed = inventory.removeItem(0);

        // then
        assertThat(removed).isPresent();
        assertThat(inventory.getAnchoredItems()).isEmpty();
        // Verify slots are freed
        ItemStack anotherPotion = new ItemStack(UUID.randomUUID(), potionDef, 1);
        assertThat(inventory.addItem(anotherPotion)).isInstanceOf(InventoryResult.Success.class);
        assertThat(inventory.getAnchoredItems()).containsKey(0);
    }
}
