package net.forge.grabthat.network;

import java.util.UUID;

import net.forge.grabthat.data.CarryData;
import net.minecraft.network.FriendlyByteBuf;

public record CarrySyncPacket(UUID playerUUID, CarryData data) {

    public static void encode(CarrySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID);
        CarryData.write(buf, msg.data);
    }

    public CarrySyncPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), CarryData.read(buf));
    }
}