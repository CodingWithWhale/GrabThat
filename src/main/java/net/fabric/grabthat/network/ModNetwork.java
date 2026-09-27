package net.fabric.grabthat.network;

import net.fabric.grabthat.GrabThat;
import net.fabric.grabthat.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(GrabThat.MODID).versioned(PROTOCOL_VERSION);

        registrar.playToServer(PickupEntityPayload.TYPE, PickupEntityPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryPayloadHandler.handlePickupEntity(context.player(), payload)));

        registrar.playToServer(PickupBlockPayload.TYPE, PickupBlockPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryPayloadHandler.handlePickupBlock(context.player(), payload)));

        registrar.playToServer(ThrowPayload.TYPE, ThrowPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryPayloadHandler.handleThrow(context.player(), payload)));

        registrar.playToServer(DropPayload.TYPE, DropPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryPayloadHandler.handleDrop(context.player(), payload)));

        registrar.playToServer(DismountSelfPayload.TYPE, DismountSelfPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryPayloadHandler.handleDismountSelf(context.player(), payload)));

        registrar.playToClient(CarrySyncPayload.TYPE, CarrySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryClientPayloadHandler.handleCarrySync(payload)));

        registrar.playToClient(CarriedLaunchPayload.TYPE, CarriedLaunchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CarryClientPayloadHandler.handleCarriedLaunch(payload)));
    }

    public static void sendCarrySync(ServerPlayer player) {
        var data = player.getData(ModAttachments.CARRY_DATA);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new CarrySyncPayload(player.getUUID(), data));
    }
}