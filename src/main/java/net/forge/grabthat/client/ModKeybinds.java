package net.forge.grabthat.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;

public class ModKeybinds {

    public static final KeyMapping GRAB_KEY = new GrabThatKeyMapping(
            "key.grabthat.grab",
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.grabthat");

    private ModKeybinds() {}
}