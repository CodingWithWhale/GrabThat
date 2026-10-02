package net.neoforge.grabthat.network;

import net.neoforge.grabthat.GrabThat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DismountSelfPayload() implements CustomPacketPayload {
    public static final Type<DismountSelfPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "dismount_self"));

    public static final StreamCodec<FriendlyByteBuf, DismountSelfPayload> STREAM_CODEC =
            StreamCodec.unit(new DismountSelfPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}