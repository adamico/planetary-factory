package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.PersonalAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Plan-request: Select Amount was confirmed, so resolve this many of this recipe.
 *
 * <p>Its answer is the Crafting Plan menu the server opens in reply, which is why there is no
 * plan-result packet beside this one.
 */
public record PlanRequestPacket(ResourceLocation recipe, int amount) implements CustomPacketPayload {

    public static final Type<PlanRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_plan_request"));

    public static final StreamCodec<ByteBuf, PlanRequestPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PlanRequestPacket::recipe,
            ByteBufCodecs.VAR_INT, PlanRequestPacket::amount,
            PlanRequestPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanRequestPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.openPlan(player, packet.recipe(), packet.amount());
        }
    }
}
