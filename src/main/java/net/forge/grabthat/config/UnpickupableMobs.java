package net.forge.grabthat.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;

public final class UnpickupableMobs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "unpickupable_mobs.json";

    private static volatile Set<String> unpickupable = Set.of();
    private static volatile boolean loaded = false;

    private UnpickupableMobs() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        load();
    }

    public static boolean isUnpickupable(EntityType<?> type) {
        if (!loaded) {
            load();
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key != null && unpickupable.contains(key.toString());
    }

    public static void load() {
        Path file = configPath();
        if (Files.notExists(file)) {
            try {
                Files.createDirectories(file.getParent());
                writeDefault(file);
            } catch (IOException ignored) {
            }
        }
        try {
            Set<String> next = new HashSet<>();
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isBoolean()
                        && entry.getValue().getAsJsonPrimitive().getAsBoolean()) {
                    next.add(entry.getKey());
                }
            }
            unpickupable = next.isEmpty() ? Set.of() : Set.copyOf(next);
        } catch (Exception ignored) {
            unpickupable = Set.of();
        } finally {
            loaded = true;
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("grabthat").resolve(FILE_NAME);
    }

    private static void writeDefault(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("minecraft:ender_dragon", true);
        root.addProperty("minecraft:wither", true);
        root.addProperty("minecraft:warden", true);
        root.addProperty("minecraft:elder_guardian", true);
        root.addProperty("minecraft:ghast", true);
        Files.writeString(file, GSON.toJson(root));
    }
}