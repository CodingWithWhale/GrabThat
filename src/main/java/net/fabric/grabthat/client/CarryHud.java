package net.fabric.grabthat.client;

import net.fabric.grabthat.GrabThat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public class CarryHud {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final ResourceLocation JUMP_BAR_BACKGROUND_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/jump_bar_background");
    private static final ResourceLocation JUMP_BAR_PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/jump_bar_progress");

    private CarryHud() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CarryHud::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GrabThat.MODID, "strength_meter");
        event.registerAboveAll(id, CarryHud::render);
    }

    private static void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (mc.options.hideGui) return;

        var data = CarryClientContext.get(mc.player.getUUID());
        if (!data.aiming()) return;

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - 34;
        int fillWidth = (int) (BAR_WIDTH * data.throwPower());

        guiGraphics.setColor(0.988f, 0.565f, 0.867f, 1.0f);
        guiGraphics.blitSprite(JUMP_BAR_BACKGROUND_SPRITE, barX, barY, 0, BAR_WIDTH, BAR_HEIGHT);
        if (fillWidth > 0) {
            guiGraphics.blitSprite(JUMP_BAR_PROGRESS_SPRITE, BAR_WIDTH, BAR_HEIGHT, 0, 0,
                    barX, barY, 0, fillWidth, BAR_HEIGHT);
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        String text = (int) (data.throwPower() * 100) + "%";
        guiGraphics.drawCenteredString(mc.font, text, screenWidth / 2, barY - 10, 0xFFFFFFFF);
    }
}
