package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.PersonalAssembler;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Start: queue the plan this player is looking at.
 *
 * <p>It carries the plan's id and nothing else. The plan itself is already on the server, so there
 * is no plan to send back and therefore nothing to re-validate -- which is the whole reason the
 * dialogs are server-opened menus (ADR-0038). The id is there so that a Start cannot land against a
 * <em>different</em> plan than the one the player was shown: a second dialog resolved in between
 * would otherwise be paid for by a click aimed at the first.
 */
public record PlanStartPacket(UUID planId) implements CustomPacketPayload {

    public static final Type<PlanStartPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_plan_start"));

    public static final StreamCodec<ByteBuf, PlanStartPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PlanStartPacket::planId,
            PlanStartPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanStartPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.start(player, packet.planId());
        }
    }
}
