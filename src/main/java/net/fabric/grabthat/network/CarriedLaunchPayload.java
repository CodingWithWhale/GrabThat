package net.fabric.grabthat.network;

import java.util.UUID;

import net.fabric.grabthat.GrabThat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CarriedLaunchPayload(
        UUID carrierUUID,
        double x, double y, double z,
        double vx, double vy, double vz) implements CustomPacketPayload {

    public static final Type<CarriedLaunchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "carried_launch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarriedLaunchPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CarriedLaunchPayload decode(RegistryFriendlyByteBuf buf) {
            return new CarriedLaunchPayload(
                    buf.readUUID(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CarriedLaunchPayload payload) {
            buf.writeUUID(payload.carrierUUID);
            buf.writeDouble(payload.x);
            buf.writeDouble(payload.y);
            buf.writeDouble(payload.z);
            buf.writeDouble(payload.vx);
            buf.writeDouble(payload.vy);
            buf.writeDouble(payload.vz);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}