package net.forge.grabthat.network;

import net.minecraft.network.FriendlyByteBuf;

public record ThrowPacket(float power) {

    public static void encode(ThrowPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.power);
    }

    public ThrowPacket(FriendlyByteBuf buf) {
        this(buf.readFloat());
    }
}