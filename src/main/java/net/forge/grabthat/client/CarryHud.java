package net.forge.grabthat.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.forge.grabthat.GrabThat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrabThat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CarryHud {

    private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BACKGROUND_V_OFFSET = 84;
    private static final int FLASH_V_OFFSET = 74;
    private static final int FILL_V_OFFSET = 89;
    private static final float FILL_RED = 0.94f;
    private static final float FILL_GREEN = 0.52f;
    private static final float FILL_BLUE = 0.96f;

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

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 34;
        int fillWidth = Math.max(0, (int) (BAR_WIDTH * data.throwPower()));

        guiGraphics.blit(GUI_ICONS_LOCATION, x, y, 0, BACKGROUND_V_OFFSET, BAR_WIDTH, BAR_HEIGHT);

        RenderSystem.setShaderColor(FILL_RED, FILL_GREEN, FILL_BLUE, 1f);
        if (data.throwPower() >= 1f) {
            guiGraphics.blit(GUI_ICONS_LOCATION, x, y, 0, FLASH_V_OFFSET, BAR_WIDTH, BAR_HEIGHT);
        } else if (fillWidth > 0) {
            guiGraphics.blit(GUI_ICONS_LOCATION, x, y, 0, FILL_V_OFFSET, fillWidth, BAR_HEIGHT);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        String text = (int) (data.throwPower() * 100) + "%";
        guiGraphics.drawCenteredString(mc.font, text, screenWidth / 2, y - 10, 0xFFFFFFFF);
    }
}