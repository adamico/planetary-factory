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
import com.planetaryfactory.core.network.FuelTablePacket;
import com.planetaryfactory.core.network.PFNetwork;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
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
 * <p><b>The client gets the table too.</b> A data pack is server truth and never reaches a client
 * on its own, so the rows are sent on datapack sync -- login and {@code /reload}, the same two
 * moments the table can change -- the way the Assembler's hand-recipe set is (ADR-0038). Without
 * it an item's fuel tooltip would be right in single-player and blank on a server, which is the
 * worse of the two failures because only one of them is ever noticed.
 *
 * <p>The arithmetic and the default-deny rule are {@link FuelTable}'s, which has no Minecraft on
 * it; this class only turns a {@link ItemStack} into the two strings that class asks for.
 */
public final class PFFuel {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    /** Replaced wholesale on reload or on sync; read from both threads. */
    private static volatile FuelTable table = FuelTable.EMPTY;

    /** The same rows, kept flat so the sync packet does not have to take the table apart. */
    private static volatile List<FuelRow> rows = List.of();

    private PFFuel() {
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    /** Login and {@code /reload}: the two moments the table can differ from what a client holds. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            send(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(PFFuel::send);
        }
    }

    private static void send(ServerPlayer player) {
        PFNetwork.sendToPlayer(player, new FuelTablePacket(rows));
    }

    /**
     * Takes a whole table, from the reload listener or from the packet.
     *
     * <p>One holder for both sides. In single-player the two arrive at the same static with the
     * same content, and a second client-side holder would be a copy to keep in step for no
     * mechanic in return.
     */
    public static void accept(List<FuelRow> loaded) {
        rows = List.copyOf(loaded);
        table = new FuelTable(rows);
    }

    /** What a burner spends per tick, for the tooltip that turns joules into seconds. */
    public static long joulesPerTick() {
        return FurnaceTier.STONE.joulesPerTick();
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
            accept(rows);
            LOG.info("Loaded {} furnace fuels from {} rows", table.size(), rows.size());
        }
    }
}
