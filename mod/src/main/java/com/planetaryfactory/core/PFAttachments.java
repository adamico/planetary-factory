package com.planetaryfactory.core;

import com.planetaryfactory.core.assembler.AssemblerCodecs;
import com.planetaryfactory.core.assembler.AssemblerQueue;
import com.planetaryfactory.core.ore.OreCodecs;
import com.planetaryfactory.core.ore.OreDelta;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

/**
 * Player-attached state the pack owns.
 *
 * <p>The Personal Assembler's queue, which belongs to a player, and the ore deltas, which belong
 * to a chunk. Both are attachments rather than world saves for the same reason: they are keyed to
 * something that already loads and unloads, and they have to outlive a logout -- a plan already
 * paid for has to still be there when the player comes back, and a patch that was mined has to
 * still be mined.
 */
public final class PFAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PlanetaryFactoryCore.NAMESPACE);

    /**
     * The Assembler queue.
     *
     * <p>{@code copyOnDeath} because death is not a reason to lose a plan that has already been paid
     * for -- the reservation left the inventory at Start, so dropping the queue on death would
     * destroy the items rather than scatter them.
     */
    public static final Supplier<AttachmentType<AssemblerQueue>> ASSEMBLER_QUEUE = ATTACHMENTS.register(
            "assembler_queue",
            () -> AttachmentType.builder(AssemblerQueue::new)
                    .serialize(AssemblerCodecs.QUEUE)
                    .copyOnDeath()
                    .build());

    /**
     * What has already been drawn out of the ore blocks in one chunk (ADR-0041).
     *
     * <p>A chunk attachment rather than a block entity per ore block: a starting field is around
     * 1150 blocks, and the delta is sparse -- an untouched field costs nothing and the entries
     * unload with their chunk. No {@code copyOnDeath}, because a chunk does not die.
     */
    public static final Supplier<AttachmentType<OreDelta>> ORE_DELTA = ATTACHMENTS.register(
            "ore_delta",
            () -> AttachmentType.builder(OreDelta::new)
                    .serialize(OreCodecs.DELTA)
                    .build());

    private PFAttachments() {
    }

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
