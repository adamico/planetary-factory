package com.planetaryfactory.core.mining.rig;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every number the two rigs are built from, as the mod sees them (#192, #193).
 *
 * <p>ADR-0043 states the rule directly -- "the 2x2 and 3x3 footprints... become extracted facts
 * rather than two integers somebody typed" -- and #193 widened it past the footprint. This class
 * therefore holds no number at all. It reads
 * {@code planetaryfactory_core/mining/drills.json}, which {@code scripts/build-rig-assets.py}
 * copies out of {@code data/factorio/machine.json}'s {@code drills} rows. Same idiom as
 * {@link com.planetaryfactory.core.ore.OreCorpus}: a classpath resource, loaded once, read at
 * block registration time, because a machine's footprint is needed before any datapack exists.
 *
 * <p>One reader rather than two. A separate class for speeds beside a
 * footprint reader would be two loaders over one file, and the second would drift.
 *
 * <p><b>What it does not do is derive.</b> {@code vector_to_place_result} and
 * {@code resource_searching_radius} are handed on as Factorio states them -- centre-relative, in
 * tiles. Turning either into a block offset is {@link RigOutputTile}'s and {@link RigArea}'s, where
 * the mod's Minecraft-free test source set can assert the arithmetic.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class RigCorpus {

    private static final String PATH = "/planetaryfactory_core/mining/drills.json";

    private static final RigCorpus INSTANCE = load(PATH);

    private final Map<RigTier, Row> rows;

    private RigCorpus(Map<RigTier, Row> rows) {
        this.rows = rows;
    }

    public static RigCorpus get() {
        return INSTANCE;
    }

    static RigCorpus load(String path) {
        try (InputStream stream = RigCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-rig-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject drills = root.getAsJsonObject("drills");
            Map<RigTier, Row> rows = new EnumMap<>(RigTier.class);
            for (RigTier tier : RigTier.values()) {
                JsonObject row = drills == null ? null : drills.getAsJsonObject(tier.factorioName());
                if (row == null) {
                    throw new IllegalStateException(
                            path + " carries no " + tier.factorioName() + " row");
                }
                rows.put(tier, read(path, tier, row));
            }
            return new RigCorpus(Map.copyOf(rows));
        } catch (IOException broken) {
            throw new IllegalStateException("could not read " + path, broken);
        }
    }

    private static Row read(String path, RigTier tier, JsonObject row) {
        JsonArray vector = row.getAsJsonArray("vector_to_place_result");
        if (vector == null || vector.size() != 2) {
            // The generator already refuses to write a row without it, so reaching here means the
            // resource was hand-edited. Naming the field beats a null becoming a rig that ejects
            // into itself.
            throw new IllegalStateException(
                    path + "'s " + tier.factorioName() + " row has no [x, y] output vector");
        }
        return new Row(
                row.get("tile_width").getAsInt(),
                row.get("tile_height").getAsInt(),
                row.get("mining_speed").getAsDouble(),
                row.get("energy_usage").getAsDouble(),
                row.get("energy_type").getAsString(),
                fuelCategories(row.get("fuel_categories")),
                vector.get(0).getAsDouble(),
                vector.get(1).getAsDouble(),
                row.get("resource_searching_radius").getAsDouble());
    }

    private static List<String> fuelCategories(JsonElement element) {
        // Null on an electric rig, which takes no fuel at all -- it is a supply-area pole customer
        // under ADR-0036 (#194).
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<String> categories = new ArrayList<>();
        for (JsonElement category : element.getAsJsonArray()) {
            categories.add(category.getAsString());
        }
        return List.copyOf(categories);
    }

    public Row rowOf(RigTier tier) {
        return rows.get(tier);
    }

    /**
     * One drill prototype, as far as the mod reads it.
     *
     * @param width along {@code facing.rightOf()} -- Factorio's {@code tile_width}
     * @param height along {@code facing} -- Factorio's {@code tile_height}
     * @param miningSpeed operations per second, against a resource's own {@code mining_time}
     * @param energyUsage watts, ADR-0047's denominator
     * @param energyType {@code burner} or {@code electric}
     * @param fuelCategories what a burner rig admits; empty on an electric one
     * @param vectorX the output vector's {@code x}, east-positive, centre-relative in tiles
     * @param vectorY the output vector's {@code y}, south-positive, centre-relative in tiles
     * @param searchingRadius how far from its centre the rig reaches, in tiles
     */
    public record Row(
            int width,
            int height,
            double miningSpeed,
            double energyUsage,
            String energyType,
            List<String> fuelCategories,
            double vectorX,
            double vectorY,
            double searchingRadius) {

        /** Whether the rig has a fuel slot. The burner rig does; ADR-0036's pole customer does not. */
        public boolean burnsFuel() {
            return "burner".equals(energyType);
        }
    }
}
