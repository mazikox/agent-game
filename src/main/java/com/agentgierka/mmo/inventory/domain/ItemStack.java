package com.agentgierka.mmo.inventory.domain;

import java.util.UUID;

/**
 * Represents a specific item or a stack of items in the player's inventory.
 * Maintains item status and enforces stack size invariants.
 */
public class ItemStack {
    private final UUID id;
    private final ItemDefinition definition;
    private int quantity;

    public ItemStack(UUID id, ItemDefinition definition, int quantity) {
        validateQuantity(quantity, definition.maxStack());
        this.id = id;
        this.definition = definition;
        this.quantity = quantity;
    }

    public boolean canStackWith(ItemStack other) {
        return this.definition.id().equals(other.definition.id()) 
            && !isFullStack();
    }

    public void addQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add must be positive");
        }
        this.quantity = Math.min(this.quantity + amount, definition.maxStack());
    }

    public int getRemainingCapacity() {
        return definition.maxStack() - quantity;
    }

    public boolean isFullStack() {
        return quantity >= definition.maxStack();
    }

    public int getWidth() {
        return definition.width();
    }

    public int getHeight() {
        return definition.height();
    }

    public UUID getId() {
        return id;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        // We allow setting to 0, which means the stack should be removed by the inventory
        this.quantity = Math.min(quantity, definition.maxStack());
    }

    public ItemStack copy() {
        return new ItemStack(this.id, this.definition, this.quantity);
    }

    private void validateQuantity(int quantity, int maxStack) {
        if (quantity <= 0 || quantity > maxStack) {
            throw new IllegalArgumentException("Invalid quantity for item: " + quantity);
        }
    }
}
