package com.planetaryfactory.core.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;

/**
 * How a chunk's ore deltas are written down, so a mined patch is still mined after a logout.
 *
 * <p>{@code Codec} is DataFixerUpper's rather than Minecraft's, exactly as the Assembler's is
 * (ADR-0038), which is what lets the round trip be a unit test. The failure that buys: a codec
 * that drops its map does not crash -- it returns a patch that silently refilled itself overnight,
 * with nothing in a log and nothing for a player to report except that mining got easier.
 *
 * <p>Positions are packed longs and are written as strings, because a JSON or NBT map key is a
 * string in both dynamic ops. The alternative -- a list of pairs -- costs more bytes per entry for
 * a map that can hold a patch's worth of them.
 */
public final class OreCodecs {

    private static final Codec<Long> POSITION = Codec.STRING.xmap(Long::parseLong, String::valueOf);

    private static final Codec<Map<Long, Integer>> DRAWN =
            Codec.unboundedMap(POSITION, Codec.INT);

    /** The chunk attachment: the positions in this chunk that have had something taken out. */
    public static final Codec<OreDelta> DELTA = RecordCodecBuilder.create(instance -> instance.group(
                    DRAWN.fieldOf("drawn").forGetter(OreDelta::drawn))
            .apply(instance, OreDelta::of));

    private OreCodecs() {
    }
}
