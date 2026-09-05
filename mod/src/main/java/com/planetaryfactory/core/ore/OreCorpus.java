package com.planetaryfactory.core.ore;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factorio's resource amounts, as the mod sees them.
 *
 * <p>ADR-0022's rule is that Factorio's numbers are extracted and never transcribed, and ADR-0041
 * puts patch totals under it. So this class holds no number: it reads
 * {@code planetaryfactory_core/ore/amounts.json}, which
 * {@code scripts/factorio-resource-extract.py} writes out of the same dump the rest of the corpus
 * comes from.
 *
 * <p><b>A classpath resource, not a datapack file.</b> The stage count sizes a blockstate property,
 * which is fixed at registration -- before any world, any datapack and any reload exists. Loading
 * it here means the ladder a block renders and the ladder Factorio ships are the same list, with no
 * step where someone keeps them in step by hand.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class OreCorpus {

    private static final String PATH = "/planetaryfactory_core/ore/amounts.json";

    private static final OreCorpus INSTANCE = load(PATH);

    private final Map<String, Resource> resources;
    private final DistanceLaw law;

    private OreCorpus(Map<String, Resource> resources, DistanceLaw law) {
        this.resources = resources;
        this.law = law;
    }

    public static OreCorpus get() {
        return INSTANCE;
    }

    static OreCorpus load(String path) {
        try (InputStream stream = OreCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/factorio-resource-extract.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, Resource> resources = new LinkedHashMap<>();
            JsonObject entries = root.getAsJsonObject("resources");
            for (String name : entries.keySet()) {
                JsonObject entry = entries.getAsJsonObject(name);
                List<Double> ratios = new ArrayList<>();
                entry.getAsJsonArray("stage_ratios").forEach(value -> ratios.add(value.getAsDouble()));
                resources.put(name, new Resource(
                        name,
                        entry.get("factorio_name").getAsString(),
                        entry.get("starting_amount").isJsonNull()
                                ? 0L
                                : (long) entry.get("starting_amount").getAsDouble(),
                        List.copyOf(ratios)));
            }
            JsonObject distance = root.getAsJsonObject("distance_law");
            return new OreCorpus(
                    Map.copyOf(resources),
                    new DistanceLaw(distance.get("offset").getAsInt(), distance.get("divisor").getAsInt()));
        } catch (IOException broken) {
            throw new IllegalStateException("could not read " + path, broken);
        }
    }

    public Resource resource(String name) {
        Resource found = resources.get(name);
        if (found == null) {
            throw new IllegalArgumentException(name + " is not one of Terra's ores: " + resources.keySet());
        }
        return found;
    }

    public Map<String, Resource> resources() {
        return resources;
    }

    /**
     * How many sprite stages every ore renders through -- Factorio's eight.
     *
     * <p>The maximum across the alphabet, because the blockstate property is one size for every
     * ore block and a resource with a shorter ladder simply never reaches the top of it.
     */
    public int stageCount() {
        return resources.values().stream()
                .mapToInt(resource -> OreStage.count(resource.stageRatios()))
                .max()
                .orElse(1);
    }

    public DistanceLaw distanceLaw() {
        return law;
    }

    /**
     * One resource: its patch total and the ladder its stages are rendered against.
     *
     * @param name the pack's block name -- {@code iron}, {@code stone}
     * @param factorioName the corpus key, which is Factorio's own (ADR-0028)
     * @param startingAmount Factorio's starting patch total, or {@code 0} where it deals none
     * @param stageRatios fractions of a block's own initial amount, richest first
     */
    public record Resource(String name, String factorioName, long startingAmount, List<Double> stageRatios) {
    }

    /**
     * Factorio's richness-by-distance term, {@code max((offset + distance) / divisor, 1)}.
     *
     * <p>Flat inside {@code divisor - offset} blocks of spawn -- 1600 of them -- which is the
     * arithmetic ADR-0041 quotes for why leaving the starting area early buys nothing. A Factorio
     * tile and a Minecraft block are both a metre, so the distance needs no conversion.
     */
    public record DistanceLaw(int offset, int divisor) {

        public double richnessAt(double distance) {
            return Math.max((offset + distance) / divisor, 1.0);
        }

        public int flatWithin() {
            return divisor - offset;
        }
    }
}
