package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.PersonalAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The tab on the inventory screen was clicked: open the panel. */
public record OpenPanelPacket() implements CustomPacketPayload {

    public static final Type<OpenPanelPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_open_panel"));

    public static final StreamCodec<ByteBuf, OpenPanelPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenPanelPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPanelPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.openPanel(player);
        }
    }
}
