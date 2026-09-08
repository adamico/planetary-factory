package com.planetaryfactory.core.mining.rig;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Breaking any part breaks the rig (#192, ADR-0043) -- the bed-and-door idiom, with one class
 * holding the teardown so {@link RigBlock} and {@link RigPartBlock} share exactly one copy of it.
 *
 * <p>Exactly one item drops regardless of which block the player actually broke:
 *
 * <ul>
 *   <li>Breaking the anchor drops it through its own loot table, the ordinary way; {@link
 *       #teardown} then only has to clear the parts, silently.</li>
 *   <li>Breaking a part carries an empty loot table (it is never obtained on its own), so {@link
 *       RigPartBlock} pops the drill item itself before calling {@link #teardown}, which clears the
 *       anchor and every other part.</li>
 * </ul>
 *
 * <p>The reentrancy guard is what keeps the second case from looping: {@link #teardown} removing
 * the anchor re-enters {@link RigBlock#onRemove}, which must not start a second teardown, and
 * removing a further part re-enters {@link RigPartBlock#onRemove}, which must not pop a second
 * item.
 */
public final class RigBreaker {

    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);

    private RigBreaker() {
    }

    /**
     * Removes every block of the rig anchored at {@code anchorPos} except {@code skip} -- the
     * position already being replaced by whatever triggered this call.
     */
    public static void teardown(Level level, BlockPos anchorPos, RigTier tier, Direction facing,
            BlockPos skip) {
        if (Boolean.TRUE.equals(IN_PROGRESS.get())) {
            return;
        }
        IN_PROGRESS.set(true);
        try {
            RigFootprints.Size size = RigFootprints.get().sizeOf(tier);
            List<RigGeometry.Offset> offsets = RigGeometry.footprint(
                    size.width(), size.height(), tier.blocksTall(), RigDirections.toRigFacing(facing));
            for (RigGeometry.Offset offset : offsets) {
                BlockPos pos = anchorPos.offset(offset.dx(), offset.dy(), offset.dz());
                if (pos.equals(skip)) {
                    continue;
                }
                if (level.getBlockState(pos).getBlock() instanceof RigBlock
                        || level.getBlockState(pos).getBlock() instanceof RigPartBlock) {
                    // Not destroyBlock: no loot, no second visit to onRemove's harvest path. The
                    // one item this whole break pays out was already handled by whichever block
                    // the player actually broke.
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }
}
