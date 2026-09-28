package net.forge.grabthat.network;

import net.minecraft.network.FriendlyByteBuf;

public record DropPacket() {

    public static void encode(DropPacket msg, FriendlyByteBuf buf) {
    }

    public DropPacket(FriendlyByteBuf buf) {
        this();
    }
}