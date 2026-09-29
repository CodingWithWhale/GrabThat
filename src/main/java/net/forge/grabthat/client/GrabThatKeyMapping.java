package net.forge.grabthat.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public class GrabThatKeyMapping extends KeyMapping {

    private static final int KEY_LEFT_SHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
    private static final int KEY_RIGHT_SHIFT = GLFW.GLFW_KEY_RIGHT_SHIFT;
    private static final int KEY_LEFT_CONTROL = GLFW.GLFW_KEY_LEFT_CONTROL;
    private static final int KEY_RIGHT_CONTROL = GLFW.GLFW_KEY_RIGHT_CONTROL;
    private static final int KEY_LEFT_ALT = GLFW.GLFW_KEY_LEFT_ALT;
    private static final int KEY_RIGHT_ALT = GLFW.GLFW_KEY_RIGHT_ALT;

    public GrabThatKeyMapping(String name, KeyModifier modifier, InputConstants.Type type, int keyCode, String category) {
        super(name, KeyConflictContext.IN_GAME, modifier, type, keyCode, category);
    }

    @Override
    public void setKeyModifierAndCode(KeyModifier keyModifier, InputConstants.Key key) {
        if (keyModifier == null && inKeybindsScreen()) {
            keyModifier = detectHeldModifier(key);
        }
        super.setKeyModifierAndCode(keyModifier, key);
    }

    @Override
    public void setKey(InputConstants.Key key) {
        if (inKeybindsScreen()) {
            super.setKeyModifierAndCode(detectHeldModifier(key), key);
        } else {
            super.setKey(key);
        }
    }

    @Override
    public boolean same(KeyMapping other) {
        return false;
    }

    private static boolean inKeybindsScreen() {
        return Minecraft.getInstance().screen instanceof KeyBindsScreen;
    }

    private static KeyModifier detectHeldModifier(InputConstants.Key target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return null;
        long window = mc.getWindow().getWindow();
        boolean shift = !isModifierKey(target, KEY_LEFT_SHIFT, KEY_RIGHT_SHIFT)
                && (InputConstants.isKeyDown(window, KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, KEY_RIGHT_SHIFT));
        boolean ctrl = !isModifierKey(target, KEY_LEFT_CONTROL, KEY_RIGHT_CONTROL)
                && (InputConstants.isKeyDown(window, KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, KEY_RIGHT_CONTROL));
        boolean alt = !isModifierKey(target, KEY_LEFT_ALT, KEY_RIGHT_ALT)
                && (InputConstants.isKeyDown(window, KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, KEY_RIGHT_ALT));
        if (shift) return KeyModifier.SHIFT;
        if (ctrl) return KeyModifier.CONTROL;
        if (alt) return KeyModifier.ALT;
        return null;
    }

    private static boolean isModifierKey(InputConstants.Key key, int left, int right) {
        return key.getType() == InputConstants.Type.KEYSYM
                && (key.getValue() == left || key.getValue() == right);
    }
}