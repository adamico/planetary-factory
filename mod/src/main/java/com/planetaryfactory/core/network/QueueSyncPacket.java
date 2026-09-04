package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.AssemblerQueue;
import com.planetaryfactory.core.assembler.CraftStep;
import com.planetaryfactory.core.assembler.ItemAmount;
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

    /**
     * One plan, as a row on the panel.
     *
     * <p>{@code stepItem} and {@code stepAmount} are what the plan is making <em>now</em>, which is
     * not usually what the plan is for: a queued transport belt spends most of its life crafting
     * iron gears, and a row naming only the belt says nothing is happening for the whole of it. It
     * is the current step's first output, and it is empty on a plan whose last step has finished.
     */
    public record Entry(
            UUID planId,
            String rootItem,
            int amount,
            String stepItem,
            int stepAmount,
            int step,
            int steps,
            float progress) {

        /** Written out by hand: eight components, and {@code StreamCodec.composite} stops at six. */
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buffer, entry) -> {
                    UUIDUtil.STREAM_CODEC.encode(buffer, entry.planId());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, entry.rootItem());
                    ByteBufCodecs.VAR_INT.encode(buffer, entry.amount());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, entry.stepItem());
                    ByteBufCodecs.VAR_INT.encode(buffer, entry.stepAmount());
                    ByteBufCodecs.VAR_INT.encode(buffer, entry.step());
                    ByteBufCodecs.VAR_INT.encode(buffer, entry.steps());
                    ByteBufCodecs.FLOAT.encode(buffer, entry.progress());
                },
                buffer -> new Entry(
                        UUIDUtil.STREAM_CODEC.decode(buffer),
                        ByteBufCodecs.STRING_UTF8.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.STRING_UTF8.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.FLOAT.decode(buffer)));

        /** Whether there is a step under way to name. */
        public boolean hasStep() {
            return !stepItem.isEmpty() && stepAmount > 0;
        }
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
            List<CraftStep> steps = entry.plan().steps();
            int index = entry.stepIndex();
            CraftStep current = index >= 0 && index < steps.size() ? steps.get(index) : null;
            ItemAmount making = current == null || current.outputs().isEmpty()
                    ? null
                    : current.outputs().get(0);
            entries.add(new Entry(
                    entry.plan().id(),
                    entry.plan().rootItem(),
                    entry.plan().amount(),
                    making == null ? "" : making.item(),
                    making == null ? 0 : making.count(),
                    index,
                    steps.size(),
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
