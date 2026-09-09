package com.planetaryfactory.core.felling;

/**
 * A position, with none of Minecraft's under it.
 *
 * <p>{@code BlockPos} would do everything this does and more, and using it would put the fill on the
 * wrong side of the pack's testing policy: the felling rule is "this pack logic computes something",
 * which is answered by a unit test running without Minecraft on the classpath. So the graph the fill
 * walks is made of these, and {@link TreeFelling} translates at the edge.
 */
public record FellPos(int x, int y, int z) {

    public FellPos offset(int dx, int dy, int dz) {
        return new FellPos(x + dx, y + dy, z + dz);
    }

    public FellPos below() {
        return offset(0, -1, 0);
    }

    public FellPos above() {
        return offset(0, 1, 0);
    }
}
