package net.neoforge.grabthat.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyModifier;

public final class ModKeybinds {
    public static final String CATEGORY = "key.categories.grabthat";

    public static final KeyMapping GRAB_KEY = new GrabThatKeyMapping(
            "key.grabthat.grab",
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    private ModKeybinds() {}
}