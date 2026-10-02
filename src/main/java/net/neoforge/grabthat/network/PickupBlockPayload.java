package net.neoforge.grabthat.network;

import net.neoforge.grabthat.GrabThat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PickupBlockPayload(BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<PickupBlockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "pickup_block"));

    public static final StreamCodec<FriendlyByteBuf, PickupBlockPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, PickupBlockPayload::blockPos, PickupBlockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
