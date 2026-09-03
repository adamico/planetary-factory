package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Personal Assembler's round trip (ADR-0038, #160).
 *
 * <p>Four of the five packets go client-to-server, which is the shape the ADR demands: the plan is
 * server truth, so the client asks and the server decides. The fifth carries the queue's display
 * view back, and nothing about a plan crosses in that direction except what is drawn.
 *
 * <p>There is no plan-result packet, because plan-result is the Crafting Plan menu's own opening
 * data -- the server opens the dialog, so the answer and the screen arrive together and cannot get
 * out of order.
 */
public final class PFNetwork {

    /** Bumped when a payload's shape changes; clients on the old shape are refused, not confused. */
    private static final String VERSION = "1";

    private PFNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(OpenPanelPacket.TYPE, OpenPanelPacket.STREAM_CODEC, OpenPanelPacket::handle);
        registrar.playToServer(SelectAmountPacket.TYPE, SelectAmountPacket.STREAM_CODEC, SelectAmountPacket::handle);
        registrar.playToServer(PlanRequestPacket.TYPE, PlanRequestPacket.STREAM_CODEC, PlanRequestPacket::handle);
        registrar.playToServer(PlanStartPacket.TYPE, PlanStartPacket.STREAM_CODEC, PlanStartPacket::handle);
        registrar.playToServer(PlanCancelPacket.TYPE, PlanCancelPacket.STREAM_CODEC, PlanCancelPacket::handle);
        registrar.playToClient(QueueSyncPacket.TYPE, QueueSyncPacket.STREAM_CODEC, QueueSyncPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
