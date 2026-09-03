package com.planetaryfactory.core;

import com.planetaryfactory.core.assembler.AssemblerCodecs;
import com.planetaryfactory.core.assembler.AssemblerQueue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

/**
 * Player-attached state the pack owns.
 *
 * <p>Just the Personal Assembler's queue so far. An attachment rather than a world save or a
 * capability because the queue belongs to a player and has to outlive their logout: a plan already
 * paid for has to still be there when they come back, or Start quietly became a way to lose items.
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

    private PFAttachments() {
    }

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
