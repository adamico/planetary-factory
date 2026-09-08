package com.planetaryfactory.core.mining.rig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * The two rigs' footprint sizes, as the mod sees them (#192).
 *
 * <p>ADR-0043 states the rule directly: "the 2x2 and 3x3 footprints... become extracted facts
 * rather than two integers somebody typed". So this class holds no number -- it reads
 * {@code planetaryfactory_core/mining/footprint.json}, which {@code scripts/build-rig-assets.py}
 * writes from {@code data/factorio/machine.json}'s {@code drills} rows (#188). Same idiom as
 * {@link com.planetaryfactory.core.ore.OreCorpus}: a classpath resource, loaded once, read at
 * block registration time.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class RigFootprints {

    private static final String PATH = "/planetaryfactory_core/mining/footprint.json";

    private static final RigFootprints INSTANCE = load(PATH);

    private final Map<RigTier, Size> sizes;

    private RigFootprints(Map<RigTier, Size> sizes) {
        this.sizes = sizes;
    }

    public static RigFootprints get() {
        return INSTANCE;
    }

    static RigFootprints load(String path) {
        try (InputStream stream = RigFootprints.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-rig-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject drills = root.getAsJsonObject("drills");
            Map<RigTier, Size> sizes = new EnumMap<>(RigTier.class);
            for (RigTier tier : RigTier.values()) {
                JsonObject row = drills.getAsJsonObject(tier.factorioName());
                if (row == null) {
                    throw new IllegalStateException(
                            path + " carries no " + tier.factorioName() + " row");
                }
                sizes.put(tier, new Size(
                        row.get("tile_width").getAsInt(),
                        row.get("tile_height").getAsInt()));
            }
            return new RigFootprints(Map.copyOf(sizes));
        } catch (IOException broken) {
            throw new IllegalStateException("could not read " + path, broken);
        }
    }

    public Size sizeOf(RigTier tier) {
        return sizes.get(tier);
    }

    /** A footprint's extent: {@code width} along {@code facing.rightOf()}, {@code height} along it. */
    public record Size(int width, int height) {
    }
}
