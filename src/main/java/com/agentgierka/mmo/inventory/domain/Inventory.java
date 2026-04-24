package com.agentgierka.mmo.inventory.domain;

import lombok.Getter;
import java.util.*;

public class Inventory {
    @Getter
    private final int width;
    @Getter
    private final int height;
    @Getter
    private final Map<Integer, ItemStack> anchoredItems = new HashMap<>();
    private final Set<Integer> occupiedSlots = new HashSet<>();

    public Inventory(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public InventoryResult addItem(ItemStack item) {
        if (item.getDefinition().isStackable()) {
            tryAutoStack(item);
        }

        if (item.getQuantity() <= 0) {
            return new InventoryResult.Success(-1, -1);
        }

        return findFirstFreeIndex(item)
            .map(index -> {
                placeItem(index, item);
                return (InventoryResult) new InventoryResult.Success(-1, index);
            })
            .orElse(new InventoryResult.NoSpace());
    }

    private void tryAutoStack(ItemStack item) {
        for (ItemStack existing : anchoredItems.values()) {
            if (existing.getDefinition().id().equals(item.getDefinition().id())) {
                int canAccept = existing.getRemainingCapacity();
                if (canAccept > 0) {
                    int toTransfer = Math.min(item.getQuantity(), canAccept);
                    existing.setQuantity(existing.getQuantity() + toTransfer);
                    item.setQuantity(item.getQuantity() - toTransfer);
                }
            }
            if (item.getQuantity() <= 0) break;
        }
    }

    private Optional<Integer> findFirstFreeIndex(ItemStack item) {
        for (int i = 0; i < width * height; i++) {
            if (fitsAt(i, item)) {
                Set<Integer> targetSlots = calculateOccupied(i, item);
                if (findCollidingAnchors(targetSlots, -1).isEmpty()) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }

    public InventoryResult processMove(int fromIndex, int toIndex) {
        ItemStack item = anchoredItems.get(fromIndex);
        if (item == null) return new InventoryResult.EmptySlot(fromIndex);

        InventoryResult boundsResult = validateBounds(toIndex, item);
        if (!(boundsResult instanceof InventoryResult.Success)) return boundsResult;

        Set<Integer> newSlots = calculateOccupied(toIndex, item);
        Set<Integer> collidingAnchors = findCollidingAnchors(newSlots, fromIndex);

        if (collidingAnchors.isEmpty()) {
            Set<Integer> oldSlots = calculateOccupied(fromIndex, item);
            performAtomicMove(fromIndex, toIndex, item, oldSlots, newSlots);
            return new InventoryResult.Success(fromIndex, toIndex);
        }

        if (collidingAnchors.size() == 1) {
            int otherAnchor = collidingAnchors.iterator().next();
            ItemStack otherItem = anchoredItems.get(otherAnchor);

            if (item.getDefinition().id().equals(otherItem.getDefinition().id()) && item.getDefinition().isStackable()) {
                return handleStack(fromIndex, otherAnchor, item, otherItem);
            }

            return handleSwap(fromIndex, toIndex, item, otherAnchor, otherItem);
        }

        return new InventoryResult.Collision(toIndex, collidingAnchors);
    }

    private InventoryResult handleStack(int fromIndex, int toIndex, ItemStack source, ItemStack target) {
        int maxStack = target.getDefinition().maxStack();
        int spaceLeft = maxStack - target.getQuantity();
        
        if (spaceLeft <= 0) return handleSwap(fromIndex, toIndex, source, toIndex, target);

        int toTransfer = Math.min(source.getQuantity(), spaceLeft);
        target.setQuantity(target.getQuantity() + toTransfer);
        source.setQuantity(source.getQuantity() - toTransfer);

        if (source.getQuantity() <= 0) {
            Set<Integer> oldSlots = calculateOccupied(fromIndex, source);
            anchoredItems.remove(fromIndex);
            occupiedSlots.removeAll(oldSlots);
        }

        return new InventoryResult.Success(fromIndex, toIndex);
    }

    private InventoryResult handleSwap(int fromIndex, int toIndex, ItemStack source, int otherAnchor, ItemStack otherItem) {
        if (!fitsAt(fromIndex, otherItem)) {
            return new InventoryResult.Collision(toIndex, Set.of(otherAnchor));
        }

        Set<Integer> sourceNewSlots = calculateOccupied(toIndex, source);
        Set<Integer> otherNewSlots = calculateOccupied(fromIndex, otherItem);

        Set<Integer> sourceOldSlots = calculateOccupied(fromIndex, source);
        Set<Integer> otherOldSlots = calculateOccupied(otherAnchor, otherItem);
        
        anchoredItems.remove(fromIndex);
        anchoredItems.remove(otherAnchor);
        occupiedSlots.removeAll(sourceOldSlots);
        occupiedSlots.removeAll(otherOldSlots);

        Set<Integer> potentialCollisions = findCollidingAnchors(otherNewSlots, -1);
        if (!potentialCollisions.isEmpty()) {
            anchoredItems.put(fromIndex, source);
            anchoredItems.put(otherAnchor, otherItem);
            occupiedSlots.addAll(sourceOldSlots);
            occupiedSlots.addAll(otherOldSlots);
            return new InventoryResult.Collision(fromIndex, potentialCollisions);
        }

        anchoredItems.put(toIndex, source);
        anchoredItems.put(fromIndex, otherItem);
        occupiedSlots.addAll(sourceNewSlots);
        occupiedSlots.addAll(otherNewSlots);

        return new InventoryResult.Success(fromIndex, toIndex);
    }

    private boolean fitsAt(int index, ItemStack item) {
        return validateBounds(index, item) instanceof InventoryResult.Success;
    }

    private Set<Integer> findCollidingAnchors(Set<Integer> targetSlots, int currentAnchor) {
        Set<Integer> colliding = new HashSet<>();
        for (Map.Entry<Integer, ItemStack> entry : anchoredItems.entrySet()) {
            if (entry.getKey() == currentAnchor) continue;
            Set<Integer> itemSlots = calculateOccupied(entry.getKey(), entry.getValue());
            if (targetSlots.stream().anyMatch(itemSlots::contains)) {
                colliding.add(entry.getKey());
            }
        }
        return colliding;
    }

    private InventoryResult validateBounds(int topLeft, ItemStack item) {
        int col = topLeft % width;
        int row = topLeft / width;
        if (col + item.getWidth() > width || row + item.getHeight() > height) {
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

    private void performAtomicMove(int from, int to, ItemStack item, Set<Integer> oldIndices, Set<Integer> newIndices) {
        occupiedSlots.removeAll(oldIndices);
        anchoredItems.remove(from);
        anchoredItems.put(to, item);
        occupiedSlots.addAll(newIndices);
    }

    public void placeItem(int index, ItemStack item) {
        anchoredItems.put(index, item);
        occupiedSlots.addAll(calculateOccupied(index, item));
    }

    public Optional<ItemStack> removeItem(int index) {
        ItemStack item = anchoredItems.remove(index);
        if (item != null) {
            Set<Integer> slots = calculateOccupied(index, item);
            occupiedSlots.removeAll(slots);
            return Optional.of(item);
        }
        return Optional.empty();
    }
}
