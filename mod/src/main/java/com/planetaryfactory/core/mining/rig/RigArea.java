package com.planetaryfactory.core.mining.rig;

import com.planetaryfactory.core.mining.rig.RigGeometry.Offset;
import java.util.ArrayList;
import java.util.List;

/**
 * Which tiles a rig works: the layer directly beneath it, sized by Factorio's own
 * {@code resource_searching_radius} (#193).
 *
 * <p>ADR-0043 states the areas as "the 2x2 beneath it" and "the 5x5 beneath it", two figures the
 * pack would otherwise have typed beside the footprints. Factorio states one instead, and a tile is
 * worked when <b>its centre lies within that radius of the drill's centre</b>. The burner rig's
 * 0.99 gives back its own 2x2; the electric rig's 2.49 gives a 5x5 that overhangs its 3x3 by one
 * tile on every side. One rule, both ladders, and #194 does not type a five.
 *
 * <p>Worth recording that the radius is <em>not</em> the number a 2x2 suggests. 0.49 was the guess
 * and the corpus corrected it -- which is the argument for reading the field rather than deriving
 * it from the footprint.
 *
 * <h2>Beneath, and only beneath</h2>
 *
 * <p>The area is the single layer under the rig's base. ADR-0043 rejects scanning the column
 * downward on the overlay's account: under a scan the rig's area would hold ore the player cannot
 * see and the renderer cannot tint, so the overlay would show an empty area over a rich vein. The
 * anchor stays at the base for the same reason -- an anchor mid-column would make "beneath" mean
 * two different things.
 *
 * <p>Free of Minecraft, per the mod's testing policy: a radius and two footprint sizes.
 */
public final class RigArea {

    /** The rig's base sits at {@code dy 0}, so the tiles it works are one block below it. */
    private static final int BENEATH = -1;

    private RigArea() {
    }

    /**
     * Every tile the rig works, anchor-relative, in a fixed order.
     *
     * <p>The order matters and is deliberately stable: the rig takes the first ore-bearing tile in
     * this list each operation, so a shifting order could abandon a half-mined block mid-patch.
     * Fixed, it works one block until that block is gone.
     *
     * @param width the footprint's {@code tile_width}
     * @param height the footprint's {@code tile_height}
     * @param searchingRadius the corpus's {@code resource_searching_radius}, in tiles
     */
    public static List<Offset> tiles(int width, int height, double searchingRadius, RigFacing facing) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("a rig footprint is at least 1x1, got "
                    + width + "x" + height);
        }
        if (!(searchingRadius > 0)) {
            throw new IllegalArgumentException(
                    "a rig reaching " + searchingRadius + " tiles works nothing");
        }
        // The footprint's own centre in anchor-relative index space: a 2x2's is 0.5, a 3x3's is 1.
        double centreLateral = (width - 1) / 2.0;
        double centreDepth = (height - 1) / 2.0;

        RigFacing right = facing.rightOf();
        List<Offset> tiles = new ArrayList<>();
        for (int depth = (int) Math.floor(centreDepth - searchingRadius);
                depth <= (int) Math.ceil(centreDepth + searchingRadius); depth++) {
            if (Math.abs(depth - centreDepth) > searchingRadius) {
                continue;
            }
            for (int lateral = (int) Math.floor(centreLateral - searchingRadius);
                    lateral <= (int) Math.ceil(centreLateral + searchingRadius); lateral++) {
                if (Math.abs(lateral - centreLateral) > searchingRadius) {
                    continue;
                }
                tiles.add(new Offset(
                        facing.dx() * depth + right.dx() * lateral,
                        BENEATH,
                        facing.dz() * depth + right.dz() * lateral));
            }
        }
        return List.copyOf(tiles);
    }
}
