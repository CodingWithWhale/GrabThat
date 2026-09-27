package net.fabric.grabthat.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum CarryType {
    NONE,
    MOB,
    PLAYER,
    BLOCK;

    public static final CarryType[] VALUES = values();

    public static final StreamCodec<RegistryFriendlyByteBuf, CarryType> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CarryType decode(RegistryFriendlyByteBuf buf) {
            int id = buf.readVarInt();
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CarryType type) {
            buf.writeVarInt(type.ordinal());
        }
    };
}
