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
 * <p><b>Neither the Boiler nor the Steam Engine exists yet.</b> #224 and #225 are separate tickets;
 * this class is the seam they will read from, not a consumer of its own fields. What it exposes
 * here is therefore the handful of numbers ADR-0048 already cites by name -- the Boiler's
 * 165 °C target and its fuel draw, the Steam Engine's own 165 °C ceiling and its fluid use -- so
 * that the resource is provably parseable before either machine is built. A wider accessor is
 * #224's and #225's to add, not this ticket's to guess at.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class SteamChainCorpus {

    private static final String PATH = "/planetaryfactory_core/fluid/steam_chain.json";

    private static final String BOILER = "boiler";
    private static final String STEAM_ENGINE = "steam-engine";

    private static final SteamChainCorpus INSTANCE = load(PATH);

    private final JsonObject boiler;
    private final JsonObject steamEngine;

    private SteamChainCorpus(JsonObject boiler, JsonObject steamEngine) {
        this.boiler = boiler;
        this.steamEngine = steamEngine;
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
            return new SteamChainCorpus(boilerRow, steamEngineRow);
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
