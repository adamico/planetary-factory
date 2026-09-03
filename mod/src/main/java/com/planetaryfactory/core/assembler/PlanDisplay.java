package com.planetaryfactory.core.assembler;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A resolved plan as the Crafting Plan dialog shows it: the flattened tree in ADR-0038's three
 * categories.
 *
 * <p>This is the plan-result half of the round trip, and it travels as the dialog menu's own opening
 * data rather than as a packet of its own -- the server opens the dialog, so the result and the
 * "here is your dialog" arrive together by construction.
 *
 * <p>{@code locked} is separate from {@code missing} on purpose: one is fixed by research and the
 * other by mining, and folding them together sends a player hunting for an item they cannot yet
 * make.
 *
 * <p>{@code planId} names the plan the server is holding, and Start quotes it back. That is what
 * stops a Start aimed at one dialog from paying for a plan resolved a moment later.
 *
 * <p>What goes in the three lists is #161's; this record is the shape it fills.
 */
public record PlanDisplay(
        UUID planId,
        ResourceLocation recipe,
        int amount,
        List<ItemAmount> toCraft,
        List<ItemAmount> missing,
        List<ItemAmount> locked,
        boolean complete) {

    /** The id an incomplete plan carries: there is no plan on the server to name. */
    public static final UUID NO_PLAN = new UUID(0L, 0L);

    private static final StreamCodec<ByteBuf, ItemAmount> ITEM_AMOUNT = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ItemAmount::item,
            ByteBufCodecs.VAR_INT, ItemAmount::count,
            ItemAmount::new);

    private static final StreamCodec<ByteBuf, List<ItemAmount>> AMOUNTS =
            ITEM_AMOUNT.apply(ByteBufCodecs.list());

    /**
     * Written out by hand because the record has seven components and {@code StreamCodec.composite}
     * stops at six. Splitting it into a nested record to fit would shape the wire format around a
     * helper's arity rather than around what the dialog shows.
     */
    public static final StreamCodec<ByteBuf, PlanDisplay> STREAM_CODEC = StreamCodec.of(
            (buffer, display) -> {
                UUIDUtil.STREAM_CODEC.encode(buffer, display.planId());
                ResourceLocation.STREAM_CODEC.encode(buffer, display.recipe());
                ByteBufCodecs.VAR_INT.encode(buffer, display.amount());
                AMOUNTS.encode(buffer, display.toCraft());
                AMOUNTS.encode(buffer, display.missing());
                AMOUNTS.encode(buffer, display.locked());
                ByteBufCodecs.BOOL.encode(buffer, display.complete());
            },
            buffer -> new PlanDisplay(
                    UUIDUtil.STREAM_CODEC.decode(buffer),
                    ResourceLocation.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    AMOUNTS.decode(buffer),
                    AMOUNTS.decode(buffer),
                    AMOUNTS.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)));

    public PlanDisplay {
        toCraft = List.copyOf(toCraft);
        missing = List.copyOf(missing);
        locked = List.copyOf(locked);
    }
}
