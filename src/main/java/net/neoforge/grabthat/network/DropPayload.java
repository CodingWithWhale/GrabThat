package net.neoforge.grabthat.network;

import net.neoforge.grabthat.GrabThat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DropPayload() implements CustomPacketPayload {
    public static final Type<DropPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "drop"));

    public static final StreamCodec<FriendlyByteBuf, DropPayload> STREAM_CODEC =
            StreamCodec.unit(new DropPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}