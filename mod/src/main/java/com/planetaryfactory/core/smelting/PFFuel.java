package com.planetaryfactory.core.smelting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

/**
 * The fuel table as the game reads it: generated datapack JSON, reloaded with the rest (ADR-0047).
 *
 * <p>The files are {@code data/planetaryfactory/fuel/*.json}, written by
 * {@code scripts/factorio-fuel-convert.py} from the Factorio corpus and {@code item-map.json}.
 * Datapack JSON rather than a constant in this jar is ADR-0015's ownership rule: what an item is
 * worth is content, and the static check then reads exactly what the game reads.
 *
 * <p>Only this pack's namespace is loaded. The folder name {@code fuel} is a plausible one for
 * another mod to use, and a foreign file landing in the pack's default-deny table would be the one
 * kind of surprise the table exists to prevent.
 *
 * <p>The arithmetic and the default-deny rule are {@link FuelTable}'s, which has no Minecraft on
 * it; this class only turns a {@link ItemStack} into the two strings that class asks for.
 */
public final class PFFuel {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    /** Replaced wholesale on reload; read from the server thread. */
    private static volatile FuelTable table = FuelTable.EMPTY;

    private PFFuel() {
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    /** What one of this stack is worth as furnace fuel, or 0 if it is not fuel at all. */
    public static long joules(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        FuelTable current = table;
        if (current.isEmpty()) {
            return 0L;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<String> tags = new ArrayList<>();
        stack.getTags().forEach(tag -> tags.add(tag.location().toString()));
        return current.joules(itemId, tags);
    }

    /** For the tests and for the checks: what is loaded right now. */
    public static FuelTable table() {
        return table;
    }

    private static final class Listener extends SimpleJsonResourceReloadListener {

        private Listener() {
            super(GSON, "fuel");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
                ProfilerFiller profiler) {
            List<FuelRow> rows = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
                if (!PlanetaryFactoryCore.NAMESPACE.equals(file.getKey().getNamespace())) {
                    continue;
                }
                JsonObject body = GsonHelper.convertToJsonObject(file.getValue(), "fuel");
                boolean tag = body.has("tag");
                rows.add(new FuelRow(
                        GsonHelper.getAsString(body, "factorio_name"),
                        GsonHelper.getAsString(body, tag ? "tag" : "item"),
                        tag,
                        GsonHelper.getAsLong(body, "fuel_value"),
                        GsonHelper.getAsString(body, "fuel_category")));
            }
            table = new FuelTable(rows);
            LOG.info("Loaded {} furnace fuels from {} rows", table.size(), rows.size());
        }
    }
}
