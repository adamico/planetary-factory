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
 * Cancel one plan, by its id.
 *
 * <p>The plan is the unit and an index would not do: the queue moves under the player as plans
 * finish, so an index cancels whatever happens to be there when the packet lands.
 */
public record PlanCancelPacket(UUID planId) implements CustomPacketPayload {

    public static final Type<PlanCancelPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_plan_cancel"));

    public static final StreamCodec<ByteBuf, PlanCancelPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PlanCancelPacket::planId,
            PlanCancelPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanCancelPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.cancel(player, packet.planId());
        }
    }
}
