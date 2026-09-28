package net.forge.grabthat.network;

import net.minecraft.network.FriendlyByteBuf;

public record DismountSelfPacket() {

    public static void encode(DismountSelfPacket msg, FriendlyByteBuf buf) {
    }

    public DismountSelfPacket(FriendlyByteBuf buf) {
        this();
    }
}