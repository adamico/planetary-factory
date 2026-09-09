package com.planetaryfactory.core.felling;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Factorio's felling rate, as the mod sees it (#205, ADR-0051).
 *
 * <p>This class holds no number. It reads {@code planetaryfactory_core/felling/trees.json}, which
 * {@code scripts/build-tree-assets.py} copies out of {@code data/factorio/tree.json} -- the same
 * idiom as {@link com.planetaryfactory.core.fluid.PumpCorpus} and
 * {@link com.planetaryfactory.core.ore.OreCorpus}: a classpath resource rather than a datapack file,
 * loaded once, because the rate is wanted before any world exists.
 *
 * <p>The prototype names are carried alongside the quotient and are not decoration. 0.1375 and
 * 0.125 are both plausible-looking tree rates -- they are {@code 0.55 / 4} and {@code 0.5 / 4}, and
 * the second belongs to Factorio's <em>dead</em> trees and to its plants. Carrying the names means a
 * reader, and {@code FellingCostTest}, can ask which prototypes back the number rather than
 * recognising it.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class TreeCorpus {

    private static final String PATH = "/planetaryfactory_core/felling/trees.json";

    private static final TreeCorpus INSTANCE = load(PATH);

    private final float secondsPerLog;
    private final List<String> rateFrom;

    private TreeCorpus(float secondsPerLog, List<String> rateFrom) {
        this.secondsPerLog = secondsPerLog;
        this.rateFrom = List.copyOf(rateFrom);
    }

    public static TreeCorpus get() {
        return INSTANCE;
    }

    static TreeCorpus load(String path) {
        try (InputStream stream = TreeCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-tree-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            float rate = root.get("seconds_per_log").getAsFloat();
            if (rate <= 0.0f) {
                throw new IllegalStateException(path + " states a rate of " + rate);
            }
            List<String> sources = new ArrayList<>();
            for (JsonElement name : root.getAsJsonArray("rate_from")) {
                sources.add(name.getAsString());
            }
            if (sources.isEmpty()) {
                throw new IllegalStateException(path + " states a rate nothing backs");
            }
            return new TreeCorpus(rate, sources);
        } catch (Exception failure) {
            throw new IllegalStateException("could not read " + path, failure);
        }
    }

    /** Seconds to fell one log: Factorio's {@code mining_time / wood}, 0.1375 today. */
    public float secondsPerLog() {
        return secondsPerLog;
    }

    /** The living Nauvis prototypes the rate is the quotient of. */
    public List<String> rateFrom() {
        return rateFrom;
    }
}
