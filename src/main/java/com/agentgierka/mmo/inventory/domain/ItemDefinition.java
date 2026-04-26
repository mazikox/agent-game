package com.agentgierka.mmo.inventory.domain;

public record ItemDefinition(
    String id,
    String name,
    int width,
    int height,
    int maxStack,
    String iconUrl
) {
    public ItemDefinition {
        validateDimensions(width, height);
        validateStackSize(maxStack);
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
    }

    private void validateStackSize(int maxStack) {
        if (maxStack <= 0) {
            throw new IllegalArgumentException("Max stack size must be greater than zero");
        }
    }

    public boolean isStackable() {
        return maxStack > 1;
    }
}
