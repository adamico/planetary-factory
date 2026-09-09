package com.planetaryfactory.core.fluid;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The Offshore Pump's numbers, as the mod sees them (#213, ADR-0050).
 *
 * <p>This class holds no number at all. It reads
 * {@code planetaryfactory_core/fluid/pumps.json}, which {@code scripts/build-pump-assets.py} copies
 * out of {@code data/factorio/machine.json}'s {@code pumps} row -- the same idiom as
 * {@link com.planetaryfactory.core.mining.rig.RigCorpus} and
 * {@link com.planetaryfactory.core.ore.OreCorpus}: a classpath resource rather than a datapack file,
 * loaded once, because the rate is wanted before any world exists.
 *
 * <p><b>What it does not do is derive.</b> {@code pumping_speed} is handed on as Factorio states it
 * -- per *Factorio* tick -- and turning it into millibuckets is {@link OffshorePumpSpec}'s, where
 * the Minecraft-free test source set can assert the two tick rates do not get confused.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class PumpCorpus {

    private static final String PATH = "/planetaryfactory_core/fluid/pumps.json";

    /** The one pump ADR-0050 authors. A second would be a second row, not a second reader. */
    private static final String OFFSHORE_PUMP = "offshore-pump";

    private static final PumpCorpus INSTANCE = load(PATH);

    private final int pumpingSpeed;
    private final String energySource;

    private PumpCorpus(int pumpingSpeed, String energySource) {
        this.pumpingSpeed = pumpingSpeed;
        this.energySource = energySource;
    }

    public static PumpCorpus get() {
        return INSTANCE;
    }

    static PumpCorpus load(String path) {
        try (InputStream stream = PumpCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-pump-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject row = root.getAsJsonObject(OFFSHORE_PUMP);
            if (row == null) {
                throw new IllegalStateException(
                        path + " carries no " + OFFSHORE_PUMP + " row -- re-run "
                                + "scripts/build-pump-assets.py");
            }
            return new PumpCorpus(
                    row.get("pumping_speed").getAsInt(),
                    row.get("energy_source").getAsString());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    /** Factorio's figure, per *Factorio* tick. {@link OffshorePumpSpec} is what converts it. */
    public int pumpingSpeed() {
        return pumpingSpeed;
    }

    /**
     * Whether the pump draws power. Factorio's is {@code void}, so this is false -- read rather
     * than assumed, so that the pack's "it takes no power" is a fact about the prototype and not
     * something nobody got round to wiring up.
     */
    public boolean takesPower() {
        return !"void".equals(energySource);
    }

    /** What one Minecraft tick may produce, in millibuckets. */
    public int milliBucketsPerTick() {
        return OffshorePumpSpec.milliBucketsPerTick(pumpingSpeed);
    }
}
