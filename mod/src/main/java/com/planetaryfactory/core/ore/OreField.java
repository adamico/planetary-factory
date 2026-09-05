package com.planetaryfactory.core.ore;

/**
 * One placed ore field, and the arithmetic that turns Factorio's patch total into a block's amount.
 *
 * <p>ADR-0041 makes the <em>patch total</em> the invariant -- Factorio states it in closed form,
 * {@code 20000 * base_density * (frequency + 1) * size}, and the corpus extracts it -- while the
 * per-block amount is whatever that total divides into across the blocks the field happens to hold.
 * So a generator that resizes a patch changes the amount without anyone editing it, and the number
 * exists in no document as a constant to fall out of step with.
 *
 * <p>The division rounds <em>down</em>. A field is allowed to hold slightly less than Factorio's
 * patch and never more: rounding up would make every field on Terra richer than the number it is
 * derived from, by an amount nobody would ever notice or state.
 *
 * <p>Free of Minecraft, like everything else in this package that carries a claim.
 */
public record OreField(String resource, long total, int blocks) {

    /**
     * The units one block of this field starts with.
     *
     * <p>Floored at one: a field so large that the quotient falls below a unit would otherwise deal
     * blocks holding nothing, which reach the player as ore that breaks on the first hit and pays
     * out nothing at all. An empty field -- no blocks -- has no amount rather than a division by
     * zero, which is the shape a caller sees when a template failed to place.
     */
    public int amountPerBlock() {
        if (blocks <= 0) {
            return 0;
        }
        return (int) Math.max(1, total / blocks);
    }
}
