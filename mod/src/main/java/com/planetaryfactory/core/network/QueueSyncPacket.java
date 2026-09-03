package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.AssemblerQueue;
import com.planetaryfactory.core.assembler.QueuedPlan;
import com.planetaryfactory.core.assembler.AssemblerQueueView;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The queue as the panel draws it: enough to show what is being made and how far along, and nothing
 * else.
 *
 * <p>Not the plans themselves. The steps, the reservation and the buffer are all server truth, and a
 * client that held them could only be believed or re-validated -- so it holds neither.
 */
public record QueueSyncPacket(List<Entry> entries, boolean blocked) implements CustomPacketPayload {

    /** One plan, as a row on the panel. */
    public record Entry(UUID planId, String rootItem, int amount, int step, int steps, float progress) {

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Entry::planId,
                ByteBufCodecs.STRING_UTF8, Entry::rootItem,
                ByteBufCodecs.VAR_INT, Entry::amount,
                ByteBufCodecs.VAR_INT, Entry::step,
                ByteBufCodecs.VAR_INT, Entry::steps,
                ByteBufCodecs.FLOAT, Entry::progress,
                Entry::new);
    }

    public static final Type<QueueSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_queue_sync"));

    public static final StreamCodec<ByteBuf, QueueSyncPacket> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), QueueSyncPacket::entries,
            ByteBufCodecs.BOOL, QueueSyncPacket::blocked,
            QueueSyncPacket::new);

    public static QueueSyncPacket of(AssemblerQueue queue) {
        List<Entry> entries = new ArrayList<>();
        for (QueuedPlan entry : queue.entries()) {
            entries.add(new Entry(
                    entry.plan().id(),
                    entry.plan().rootItem(),
                    entry.plan().amount(),
                    entry.stepIndex(),
                    entry.plan().steps().size(),
                    entry.progress()));
        }
        return new QueueSyncPacket(List.copyOf(entries), queue.isBlocked());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QueueSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> AssemblerQueueView.accept(packet));
    }
}
