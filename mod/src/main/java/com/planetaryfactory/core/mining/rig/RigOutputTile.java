package com.planetaryfactory.core.mining.rig;

import com.planetaryfactory.core.mining.rig.RigGeometry.Offset;

/**
 * Which tile a rig hands its ore to, derived from Factorio's own {@code vector_to_place_result}
 * (#193).
 *
 * <p>ADR-0043 says a rig ejects onto "the tile it faces". On the electric rig's 3x3 that names one
 * tile; on the burner rig's <b>2x2 it names two</b>, and the pack would have had to pick. Factorio
 * picks: the burner drill's vector is {@code [-0.35, -1.3]}, which is one tile past the front edge
 * in the <em>left</em> column rather than astride the footprint.
 *
 * <p><b>Auto-output is a property of the prototype, not a rule about machines.</b> Only a handful
 * of entities carry this vector at all -- the mining drills and the recycler -- while a furnace or
 * an assembler has none and is emptied by an inserter. The pack's own furnace agrees: its item
 * handler permits extraction and pushes nothing.
 *
 * <h2>The conversion</h2>
 *
 * <p>Factorio states the vector centre-relative in tiles, in the prototype's own north-facing
 * frame, with {@code +x} to the east and {@code +y} to the south. A footprint of {@code width}
 * spans {@code [-width/2, +width/2]} about that centre, so tile centres sit at
 * {@code -width/2 + 0.5 + i}, and the index of the tile a coordinate falls in is
 * {@code floor(coordinate + width/2)}.
 *
 * <p>{@link RigGeometry} measures from the anchor -- the near-left corner -- with depth running
 * along the facing and lateral running to the player's right. Lateral is therefore that index
 * directly; depth is it counted from the other end, because Factorio's {@code +y} runs backwards
 * where depth runs forwards.
 *
 * <p>Free of Minecraft: two doubles and two ints. The generator that copies the vector out of the
 * corpus deliberately does not do this arithmetic, because a derivation buried in a generator is a
 * derivation nothing checks.
 */
public final class RigOutputTile {

    private RigOutputTile() {
    }

    /**
     * The offset from the anchor of the one tile this rig ejects onto, at the rig's base.
     *
     * @param width the footprint's {@code tile_width}
     * @param height the footprint's {@code tile_height}
     * @param vectorX the corpus vector's {@code x}, east-positive
     * @param vectorY the corpus vector's {@code y}, south-positive
     */
    public static Offset of(int width, int height, double vectorX, double vectorY, RigFacing facing) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("a rig footprint is at least 1x1, got "
                    + width + "x" + height);
        }
        int lateral = (int) Math.floor(vectorX + width / 2.0);
        // Factorio's +y runs south -- backwards, where the footprint's depth runs forwards along
        // the facing -- so the index is counted from the far end rather than used as it stands.
        int depth = (height - 1) - (int) Math.floor(vectorY + height / 2.0);

        RigFacing right = facing.rightOf();
        return new Offset(
                facing.dx() * depth + right.dx() * lateral,
                // Factorio is played on a plane and the vector carries no height. The tile a rig
                // feeds is at its base, which is where a furnace placed in front of it stands --
                // not on the rig's roof.
                0,
                facing.dz() * depth + right.dz() * lateral);
    }
}
