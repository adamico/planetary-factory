package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.HandRecipeSet;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Which recipes the Assembler can plan, so EMI's fill button is only offered where it leads
 * somewhere.
 *
 * <p>Sent on datapack sync, which is login and {@code /reload} both -- the same two moments the hand
 * set can change, because it is derived from the loaded recipes and nothing else.
 *
 * <p>Ids and no more. The plan, its cost and its shortfalls are re-derived per player on the server
 * and never cross in this direction.
 */
public record HandRecipeSetPacket(List<String> ids) implements CustomPacketPayload {

    public static final Type<HandRecipeSetPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_hand_recipes"));

    public static final StreamCodec<ByteBuf, HandRecipeSetPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), HandRecipeSetPacket::ids,
            HandRecipeSetPacket::new);

    public static HandRecipeSetPacket of(Set<String> ids) {
        return new HandRecipeSetPacket(List.copyOf(ids));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HandRecipeSetPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> HandRecipeSet.accept(Set.copyOf(packet.ids())));
    }
}
