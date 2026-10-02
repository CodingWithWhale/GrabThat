package net.neoforge.grabthat.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

public class GrabThatKeyMapping extends KeyMapping {

    public GrabThatKeyMapping(String name, KeyModifier modifier, InputConstants.Type type, int keyCode, String category) {
        super(name, KeyConflictContext.IN_GAME, modifier, type, keyCode, category);
    }

    @Override
    public boolean same(KeyMapping other) {
        return false;
    }

    @Override
    public boolean hasKeyModifierConflict(KeyMapping other) {
        return false;
    }
}