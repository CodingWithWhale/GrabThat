package net.forge.grabthat.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

public final class ModKeybinds {
    public static final String CATEGORY = "key.categories.grabthat";

    public static final KeyMapping GRAB_KEY = new GrabThatKeyMapping(
            "key.grabthat.grab",
            net.minecraftforge.client.settings.KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    private ModKeybinds() {}
}