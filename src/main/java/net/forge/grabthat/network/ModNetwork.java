package net.forge.grabthat.network;

import java.util.Optional;
import java.util.function.Supplier;

import net.forge.grabthat.GrabThat;
import net.forge.grabthat.data.CarryData;
import net.forge.grabthat.storage.CarryDataStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;

    private ModNetwork() {}

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(GrabThat.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

        int id = 0;
        channel.registerMessage(id++, PickupEntityPacket.class,
                PickupEntityPacket::encode, PickupEntityPacket::new,
                ModNetwork::handlePickupEntity, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, PickupBlockPacket.class,
                PickupBlockPacket::encode, PickupBlockPacket::new,
                ModNetwork::handlePickupBlock, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, ThrowPacket.class,
                ThrowPacket::encode, ThrowPacket::new,
                ModNetwork::handleThrow, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, DropPacket.class,
                DropPacket::encode, DropPacket::new,
                ModNetwork::handleDrop, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, DismountSelfPacket.class,
                DismountSelfPacket::encode, DismountSelfPacket::new,
                ModNetwork::handleDismountSelf, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, CarrySyncPacket.class,
                CarrySyncPacket::encode, CarrySyncPacket::new,
                ModNetwork::handleCarrySync, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(id++, CarriedLaunchPacket.class,
                CarriedLaunchPacket::encode, CarriedLaunchPacket::new,
                ModNetwork::handleCarriedLaunch, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static void handlePickupEntity(PickupEntityPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryPayloadHandler.handlePickupEntity(ctx.getSender(), msg));
        ctx.setPacketHandled(true);
    }

    private static void handlePickupBlock(PickupBlockPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryPayloadHandler.handlePickupBlock(ctx.getSender(), msg));
        ctx.setPacketHandled(true);
    }

    private static void handleThrow(ThrowPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryPayloadHandler.handleThrow(ctx.getSender(), msg));
        ctx.setPacketHandled(true);
    }

    private static void handleDrop(DropPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryPayloadHandler.handleDrop(ctx.getSender(), msg));
        ctx.setPacketHandled(true);
    }

    private static void handleDismountSelf(DismountSelfPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryPayloadHandler.handleDismountSelf(ctx.getSender(), msg));
        ctx.setPacketHandled(true);
    }

    private static void handleCarrySync(CarrySyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryClientPayloadHandler.handleCarrySync(msg));
        ctx.setPacketHandled(true);
    }

    private static void handleCarriedLaunch(CarriedLaunchPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CarryClientPayloadHandler.handleCarriedLaunch(msg));
        ctx.setPacketHandled(true);
    }

    public static void sendCarrySync(ServerPlayer player) {
        CarryData data = CarryDataStorage.get(player);
        channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new CarrySyncPacket(player.getUUID(), data));
    }

    public static void sendToPlayer(ServerPlayer player, CarriedLaunchPacket msg) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToServer(Object msg) {
        channel.sendToServer(msg);
    }

    public static SimpleChannel channel() {
        return channel;
    }
}