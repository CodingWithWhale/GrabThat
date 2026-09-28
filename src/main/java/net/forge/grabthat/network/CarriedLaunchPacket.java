package net.forge.grabthat.network;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;

public record CarriedLaunchPacket(
        UUID carrierUUID,
        double x, double y, double z,
        double vx, double vy, double vz) {

    public static void encode(CarriedLaunchPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.carrierUUID);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeDouble(msg.vx);
        buf.writeDouble(msg.vy);
        buf.writeDouble(msg.vz);
    }

    public CarriedLaunchPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}