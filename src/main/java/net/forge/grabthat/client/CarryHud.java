package net.forge.grabthat.client;

import net.forge.grabthat.GrabThat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrabThat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CarryHud {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BACKGROUND_COLOR = 0xFF000000;
    private static final int PROGRESS_COLOR = 0xFFFC90DD;

    private CarryHud() {}

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("strength_meter", CarryHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick,
                               int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (mc.options.hideGui) return;

        var data = CarryClientContext.get(mc.player.getUUID());
        if (!data.aiming()) return;

        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - 34;
        int fillWidth = Math.max(0, (int) (BAR_WIDTH * data.throwPower()));

        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BACKGROUND_COLOR);
        if (fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, PROGRESS_COLOR);
        }

        String text = (int) (data.throwPower() * 100) + "%";
        guiGraphics.drawCenteredString(mc.font, text, screenWidth / 2, barY - 10, 0xFFFFFFFF);
    }
}