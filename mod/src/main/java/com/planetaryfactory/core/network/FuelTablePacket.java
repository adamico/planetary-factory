package com.planetaryfactory.core.network;

import java.util.List;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.smelting.FuelRow;
import com.planetaryfactory.core.smelting.PFFuel;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The fuel table, server to client, so an item can say what it is worth as fuel (ADR-0047).
 *
 * <p>Sent on datapack sync -- login and {@code /reload} both -- which is the same moment and the
 * same reason as {@link HandRecipeSetPacket}: the table is derived from the loaded data pack and
 * from nothing else, and a data pack does not reach a client on its own.
 *
 * <p>Whole rows rather than a rendered line of text. What a client draws is a client's business,
 * and a category the client cannot see would make {@code FuelTable}'s default-deny rule mean two
 * different things on the two sides.
 */
public record FuelTablePacket(List<FuelRow> rows) implements CustomPacketPayload {

    public static final Type<FuelTablePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "fuel_table"));

    private static final StreamCodec<ByteBuf, FuelRow> ROW = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FuelRow::factorioName,
            ByteBufCodecs.STRING_UTF8, FuelRow::target,
            ByteBufCodecs.BOOL, FuelRow::tag,
            ByteBufCodecs.VAR_LONG, FuelRow::fuelValue,
            ByteBufCodecs.STRING_UTF8, FuelRow::fuelCategory,
            FuelRow::new);

    public static final StreamCodec<ByteBuf, FuelTablePacket> STREAM_CODEC = StreamCodec.composite(
            ROW.apply(ByteBufCodecs.list()), FuelTablePacket::rows,
            FuelTablePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FuelTablePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> PFFuel.accept(packet.rows()));
    }
}
