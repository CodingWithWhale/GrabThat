package net.neoforge.grabthat.client;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.neoforge.grabthat.data.CarryData;

public class CarryClientContext {
    private static final ConcurrentHashMap<UUID, CarryData> CARRY_DATA = new ConcurrentHashMap<>();

    private CarryClientContext() {}

    public static void set(UUID playerUUID, CarryData data) {
        if (data.isEmpty()) {
            CARRY_DATA.remove(playerUUID);
        } else {
            CARRY_DATA.put(playerUUID, data);
        }
    }

    public static CarryData get(UUID playerUUID) {
        return CARRY_DATA.getOrDefault(playerUUID, CarryData.EMPTY);
    }

    public static void clear() {
        CARRY_DATA.clear();
    }
}