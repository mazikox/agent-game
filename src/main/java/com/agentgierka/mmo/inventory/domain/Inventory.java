package com.agentgierka.mmo.inventory.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregate Root for the inventory system.
 * Manages item positions and maintains grid invariants.
 */
public class Inventory {
    private final int width;
    private final int height;
    private final Map<Integer, ItemStack> anchoredItems;
    private final Set<Integer> occupiedSlots;

    public Inventory(int width, int height) {
        this.width = width;
        this.height = height;
        this.anchoredItems = new HashMap<>();
        this.occupiedSlots = new HashSet<>();
    }

    public InventoryResult processMove(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return new InventoryResult.Success(fromIndex, toIndex);
        }
        if (isIndexOutOfBounds(fromIndex) || isIndexOutOfBounds(toIndex)) {
            return new InventoryResult.OutOfBounds(toIndex, 0, 0);
        }
        return moveItem(fromIndex, toIndex);
    }

    private InventoryResult moveItem(int fromIndex, int toIndex) {
        ItemStack item = anchoredItems.get(fromIndex);
        if (item == null) {
            return new InventoryResult.OutOfBounds(fromIndex, 0, 0);
        }

        InventoryResult boundsCheck = validateBounds(toIndex, item);
        if (!(boundsCheck instanceof InventoryResult.Success)) {
            return boundsCheck;
        }

        Set<Integer> newOccupied = calculateOccupied(toIndex, item);
        Set<Integer> currentOccupied = calculateOccupied(fromIndex, item);

        Set<Integer> collision = findCollisions(newOccupied, currentOccupied);
        if (!collision.isEmpty()) {
            return new InventoryResult.Collision(toIndex, collision);
        }

        performAtomicMove(fromIndex, toIndex, item, currentOccupied, newOccupied);
        return new InventoryResult.Success(fromIndex, toIndex);
    }

    private InventoryResult validateBounds(int topLeft, ItemStack item) {
        int col = topLeft % width;
        int row = topLeft / width;

        if (col + item.getWidth() > width) {
            return new InventoryResult.OutOfBounds(topLeft, item.getWidth(), item.getHeight());
        }
        if (row + item.getHeight() > height) {
            return new InventoryResult.OutOfBounds(topLeft, item.getWidth(), item.getHeight());
        }
        return new InventoryResult.Success(topLeft, topLeft);
    }

    private Set<Integer> calculateOccupied(int topLeft, ItemStack item) {
        Set<Integer> occupied = new HashSet<>();
        int col = topLeft % width;
        int row = topLeft / width;

        for (int r = 0; r < item.getHeight(); r++) {
            for (int c = 0; c < item.getWidth(); c++) {
                occupied.add((row + r) * width + (col + c));
            }
        }
        return occupied;
    }

    private Set<Integer> findCollisions(Set<Integer> newOccupied, Set<Integer> currentOccupied) {
        return newOccupied.stream()
            .filter(idx -> occupiedSlots.contains(idx) && !currentOccupied.contains(idx))
            .collect(Collectors.toSet());
    }

    private void performAtomicMove(int from, int to, ItemStack item, Set<Integer> oldIndices, Set<Integer> newIndices) {
        occupiedSlots.removeAll(oldIndices);
        anchoredItems.remove(from);
        
        anchoredItems.put(to, item);
        occupiedSlots.addAll(newIndices);
    }

    private boolean isIndexOutOfBounds(int index) {
        return index < 0 || index >= width * height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Map<Integer, ItemStack> getAnchoredItems() {
        return Collections.unmodifiableMap(anchoredItems);
    }
}
