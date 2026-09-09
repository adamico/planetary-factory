package com.planetaryfactory.core.start;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How the once-per-player flag is written down (#203).
 *
 * <p>DataFixerUpper's {@code Codec}, not Minecraft's helpers, for the same reason
 * {@code AssemblerCodecs} is: the round trip is then an ordinary unit test. A flag that fails to
 * persist reaches the player as a second starting kit on their next login and logs nothing.
 */
public final class StartingCodecs {

    public static final Codec<StartingGrant> GRANT = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("granted").forGetter(StartingGrant::granted))
            .apply(instance, StartingGrant::new));

    private StartingCodecs() {
    }
}
