package net.fabric.grabthat.network;

import net.fabric.grabthat.GrabThat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ThrowPayload(float power) implements CustomPacketPayload {
    public static final Type<ThrowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "throw"));

    public static final StreamCodec<FriendlyByteBuf, ThrowPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, ThrowPayload::power, ThrowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
