package net.forge.grabthat.client;

import net.forge.grabthat.GrabThat;
import net.forge.grabthat.config.PickupConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GrabThat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GrabThatClient {

    private GrabThatClient() {}

    public static void init(FMLClientSetupEvent event) {
        PickupConfig.init();

        CarryClientEvents.register();
        CarryRenderer.register();
        CarryPlayerEvents.register();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.GRAB_KEY);
    }
}