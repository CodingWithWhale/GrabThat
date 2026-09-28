package net.forge.grabthat.storage;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.forge.grabthat.data.CarryData;
import net.minecraft.server.level.ServerPlayer;

public final class CarryDataStorage {
    private static final ConcurrentHashMap<UUID, CarryData> DATA = new ConcurrentHashMap<>();

    private CarryDataStorage() {}

    public static CarryData get(ServerPlayer player) {
        return DATA.getOrDefault(player.getUUID(), CarryData.EMPTY);
    }

    public static void set(ServerPlayer player, CarryData data) {
        if (data.isEmpty()) {
            DATA.remove(player.getUUID());
        } else {
            DATA.put(player.getUUID(), data);
        }
    }

    public static void clear(ServerPlayer player) {
        DATA.remove(player.getUUID());
    }
}