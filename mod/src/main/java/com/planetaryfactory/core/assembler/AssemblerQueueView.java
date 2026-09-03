package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.List;

/**
 * The client's copy of the queue: what the last sync said, and nothing more.
 *
 * <p>A read-only picture, deliberately. The client never holds a plan, never computes a
 * reservation and never decides anything -- it draws rows and sends button presses, and every answer
 * comes back over the wire.
 *
 * <p>Free of {@code net.minecraft.client} on purpose, so a dedicated server can load the packet's
 * handler class without reaching for a class that is not there.
 */
public final class AssemblerQueueView {

    private static volatile QueueSyncPacket latest = new QueueSyncPacket(List.of(), false);

    private AssemblerQueueView() {
    }

    public static void accept(QueueSyncPacket packet) {
        latest = packet;
    }

    public static List<QueueSyncPacket.Entry> entries() {
        return latest.entries();
    }

    public static boolean blocked() {
        return latest.blocked();
    }
}
