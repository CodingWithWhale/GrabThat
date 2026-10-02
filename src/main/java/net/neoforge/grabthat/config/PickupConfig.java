package net.neoforge.grabthat.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.neoforged.fml.loading.FMLPaths;

public final class PickupConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "config.json";
    private static boolean enabled = true;

    private PickupConfig() {}

    public static void init() {
        Path file = configPath();
        if (Files.notExists(file)) {
            try {
                Files.createDirectories(file.getParent());
                writeDefault(file);
            } catch (IOException ignored) {
            }
        }
    }

    public static boolean isEnabled() {
        Path file = configPath();
        if (Files.exists(file)) {
            try {
                JsonConfig config = GSON.fromJson(Files.readString(file), JsonConfig.class);
                if (config != null) {
                    enabled = config.pickupKeybindEnabled;
                }
            } catch (JsonParseException | IOException ignored) {
            }
        } else {
            init();
        }
        return enabled;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("grabthat").resolve(FILE_NAME);
    }

    private static void writeDefault(Path file) throws IOException {
        JsonConfig config = new JsonConfig();
        config.pickupKeybindEnabled = true;
        Files.writeString(file, GSON.toJson(config));
    }

    private static class JsonConfig {
        boolean pickupKeybindEnabled = true;
    }
}