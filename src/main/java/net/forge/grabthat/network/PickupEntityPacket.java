package net.forge.grabthat.network;

import net.minecraft.network.FriendlyByteBuf;

public record PickupEntityPacket(int targetEntityId) {

    public static void encode(PickupEntityPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.targetEntityId);
    }

    public PickupEntityPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }
}