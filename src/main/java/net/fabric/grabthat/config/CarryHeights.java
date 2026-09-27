package net.fabric.grabthat.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class CarryHeights {
    private static final String RESOURCE = "/assets/grabthat/data/carry_heights.json";

    private static volatile Map<String, Double> heights = Map.of();
    private static volatile boolean loaded = false;

    private CarryHeights() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        load();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("grabthat")
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            load();
                            ctx.getSource().sendSuccess(() -> Component.literal("Carry heights reloaded"), true);
                            return 1;
                        })));
    }

    public static Double get(EntityType<?> type) {
        if (!loaded) {
            load();
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null ? null : heights.get(key.toString());
    }

    public static void load() {
        synchronized (CarryHeights.class) {
            try {
                TreeMap<String, Double> next = new TreeMap<>();
                try (InputStream stream = CarryHeights.class.getResourceAsStream(RESOURCE)) {
                    if (stream != null) {
                        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                                JsonElement value = entry.getValue();
                                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                                    next.put(entry.getKey(), value.getAsDouble());
                                }
                            }
                        }
                    }
                }
                heights = next;
            } catch (Exception ignored) {
                heights = Map.of();
            } finally {
                loaded = true;
            }
        }
    }
}