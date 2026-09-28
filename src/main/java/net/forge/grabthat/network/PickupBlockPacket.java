package net.forge.grabthat.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record PickupBlockPacket(BlockPos blockPos) {

    public static void encode(PickupBlockPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos);
    }

    public PickupBlockPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }
}