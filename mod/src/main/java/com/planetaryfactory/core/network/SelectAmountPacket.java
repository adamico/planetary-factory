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
 * EMI's {@code + Fill Recipe} was pressed: open Select Amount for this recipe.
 *
 * <p>{@code amount} is {@code EmiCraftContext.getAmount()} verbatim -- {@code 1} on a click and
 * {@code Integer.MAX_VALUE} on a shift-click. The server clamps it against what the resolver says is
 * affordable, so the extreme value is a request for "all" rather than a number anybody believes.
 */
public record SelectAmountPacket(ResourceLocation recipe, int amount) implements CustomPacketPayload {

    public static final Type<SelectAmountPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_select_amount"));

    public static final StreamCodec<ByteBuf, SelectAmountPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SelectAmountPacket::recipe,
            ByteBufCodecs.VAR_INT, SelectAmountPacket::amount,
            SelectAmountPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectAmountPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.openSelectAmount(player, packet.recipe(), packet.amount());
        }
    }
}
