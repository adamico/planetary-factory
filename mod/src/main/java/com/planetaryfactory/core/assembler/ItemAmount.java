package com.planetaryfactory.core.assembler;

/**
 * A count of one item, named by its registry id as a string.
 *
 * <p>A string and not a {@code ResourceLocation} or an {@code ItemStack} on purpose: everything the
 * Assembler's queue does with an item is counting it, and keeping the identity a string is what lets
 * the queue -- where this ticket's real risk lives -- be a unit test rather than a world load.
 * {@code InventoryPlayerItems} is the one place the string meets Minecraft.
 */
public record ItemAmount(String item, int count) {

    public ItemAmount {
        if (item == null || item.isBlank()) {
            throw new IllegalArgumentException("an item amount needs an item id");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("an item amount of " + count + " is not an amount");
        }
    }
}
