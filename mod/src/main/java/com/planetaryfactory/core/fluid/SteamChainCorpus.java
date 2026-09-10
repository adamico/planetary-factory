package com.planetaryfactory.core.fluid;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Terra's Boiler and Steam Engine, as the mod will eventually see them (#223, ADR-0048).
 *
 * <p>This class holds no number at all. It reads {@code planetaryfactory_core/fluid/steam_chain.json},
 * which {@code scripts/build-steam-assets.py} copies -- whole rows, every field -- out of
 * {@code data/factorio/machine.json}'s {@code boilers} and {@code generators} arrays. The same
 * idiom as {@link PumpCorpus} and {@link com.planetaryfactory.core.mining.rig.RigCorpus}: a
 * classpath resource rather than a datapack file, loaded once, so the numbers exist before any
 * world does.
 *
 * <p><b>The Boiler reads it; the Steam Engine does not exist yet.</b> #224 landed the first half
 * of the chain, so the target temperature, the fuel draw and both fluid boxes now have a consumer
 * -- {@link BoilerSpec} through {@link BoilerBlockEntity}. The Steam Engine's own fields are still
 * unconsumed and stay exposed for #225 to pick up.
 *
 * <p>The {@code fluids} rows are #224's addition and are Factorio's fluid prototypes rather than
 * this pack's: the Boiler's rate is a temperature rise paid for at <em>steam's</em> heat capacity,
 * and both that constant and water's default temperature are read here rather than typed into
 * Java. Which of the two capacities governs is {@link BoilerSpec}'s to say.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class SteamChainCorpus {

    private static final String PATH = "/planetaryfactory_core/fluid/steam_chain.json";

    private static final String BOILER = "boiler";
    private static final String STEAM_ENGINE = "steam-engine";
    private static final String FLUIDS = "fluids";

    private static final SteamChainCorpus INSTANCE = load(PATH);

    private final JsonObject boiler;
    private final JsonObject steamEngine;
    private final JsonObject fluids;

    private SteamChainCorpus(JsonObject boiler, JsonObject steamEngine, JsonObject fluids) {
        this.boiler = boiler;
        this.steamEngine = steamEngine;
        this.fluids = fluids;
    }

    public static SteamChainCorpus get() {
        return INSTANCE;
    }

    static SteamChainCorpus load(String path) {
        try (InputStream stream = SteamChainCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-steam-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject boilerRow = root.getAsJsonObject(BOILER);
            if (boilerRow == null) {
                throw new IllegalStateException(
                        path + " carries no " + BOILER + " row -- re-run scripts/build-steam-assets.py");
            }
            JsonObject steamEngineRow = root.getAsJsonObject(STEAM_ENGINE);
            if (steamEngineRow == null) {
                throw new IllegalStateException(
                        path + " carries no " + STEAM_ENGINE
                                + " row -- re-run scripts/build-steam-assets.py");
            }
            JsonObject fluidRows = root.getAsJsonObject(FLUIDS);
            if (fluidRows == null) {
                throw new IllegalStateException(
                        path + " carries no " + FLUIDS
                                + " rows -- re-run scripts/build-steam-assets.py");
            }
            return new SteamChainCorpus(boilerRow, steamEngineRow, fluidRows);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    /** The Boiler's target output temperature, in Factorio's own Celsius. ADR-0048's 165 °C. */
    public int boilerTargetTemperature() {
        return boiler.get("target_temperature").getAsInt();
    }

    /** The Boiler's fuel draw, in joules -- {@code fuel_value * effectivity} under ADR-0047. */
    public double boilerEnergyConsumption() {
        return boiler.get("energy_consumption").getAsDouble();
    }

    /** The Steam Engine's maximum input temperature. Also 165 °C -- one boiler tier, ADR-0048. */
    public int steamEngineMaximumTemperature() {
        return steamEngine.get("maximum_temperature").getAsInt();
    }

    /** How much steam the Steam Engine draws per Factorio tick. */
    public double steamEngineFluidUsagePerTick() {
        return steamEngine.get("fluid_usage_per_tick").getAsDouble();
    }

    /**
     * How much of a fluid box the Boiler holds, in Factorio units and so in millibuckets.
     *
     * <p>{@code production_type} rather than the box's name: the input box is called
     * {@code fluid_box} and the output {@code output_fluid_box}, which is a naming an ordinary
     * prototype change could swap without changing what either box is for.
     */
    public int boilerFluidBoxVolume(String productionType) {
        for (com.google.gson.JsonElement box : boiler.getAsJsonArray("fluid_boxes")) {
            JsonObject row = box.getAsJsonObject();
            if (productionType.equals(row.get("production_type").getAsString())) {
                return row.get("volume").getAsInt();
            }
        }
        throw new IllegalStateException(
                "the boiler row has no " + productionType + " fluid box -- re-run "
                        + "scripts/build-steam-assets.py");
    }

    /**
     * A fluid's heat capacity, in joules per unit per degree.
     *
     * <p>Steam's and water's differ by a factor of ten and both are plausible numbers to reach for
     * here; which one governs the Boiler is {@link BoilerSpec}'s to say, not this class's.
     */
    public double fluidHeatCapacity(String fluid) {
        return fluidRow(fluid).get("heat_capacity").getAsDouble();
    }

    /** The temperature a fluid is at when nothing has heated it. Factorio's 15 °C. */
    public int fluidDefaultTemperature(String fluid) {
        return fluidRow(fluid).get("default_temperature").getAsInt();
    }

    private JsonObject fluidRow(String fluid) {
        JsonObject row = fluids.getAsJsonObject(fluid);
        if (row == null) {
            throw new IllegalStateException(
                    "no " + fluid + " row in " + PATH + " -- re-run scripts/build-steam-assets.py");
        }
        return row;
    }

    /**
     * The raw boiler row, for a field #224 needs that this class does not yet expose by name.
     *
     * <p>Deliberately escape-hatch shaped: the resource copies the whole row, and this class is not
     * meant to gatekeep which of its fields a later ticket may read.
     */
    public JsonObject boilerRow() {
        return boiler;
    }

    /** The raw steam-engine row, for the same reason {@link #boilerRow()} exists. */
    public JsonObject steamEngineRow() {
        return steamEngine;
    }
}
