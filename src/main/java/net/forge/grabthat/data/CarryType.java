package net.forge.grabthat.data;

import net.minecraft.network.FriendlyByteBuf;

public enum CarryType {
    NONE,
    MOB,
    PLAYER,
    BLOCK;

    public static final CarryType[] VALUES = values();

    public static void write(FriendlyByteBuf buf, CarryType type) {
        buf.writeVarInt(type.ordinal());
    }

    public static CarryType read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }
}