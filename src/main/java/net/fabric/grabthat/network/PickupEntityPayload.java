package net.fabric.grabthat.network;

import net.fabric.grabthat.GrabThat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PickupEntityPayload(int targetEntityId) implements CustomPacketPayload {
    public static final Type<PickupEntityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "pickup_entity"));

    public static final StreamCodec<FriendlyByteBuf, PickupEntityPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, PickupEntityPayload::targetEntityId, PickupEntityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
