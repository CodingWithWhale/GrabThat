package net.neoforge.grabthat.network;

import java.util.UUID;

import net.neoforge.grabthat.GrabThat;
import net.neoforge.grabthat.data.CarryData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CarrySyncPayload(UUID playerUUID, CarryData data) implements CustomPacketPayload {
    public static final Type<CarrySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "carry_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarrySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CarrySyncPayload decode(RegistryFriendlyByteBuf buf) {
            UUID uuid = buf.readUUID();
            CarryData data = CarryData.STREAM_CODEC.decode(buf);
            return new CarrySyncPayload(uuid, data);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CarrySyncPayload payload) {
            buf.writeUUID(payload.playerUUID);
            CarryData.STREAM_CODEC.encode(buf, payload.data);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
