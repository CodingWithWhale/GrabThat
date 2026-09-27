package net.fabric.grabthat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.fabric.grabthat.client.CarryClientEvents;
import net.fabric.grabthat.client.CarryHud;
import net.fabric.grabthat.client.CarryPlayerEvents;
import net.fabric.grabthat.client.CarryRenderer;
import net.fabric.grabthat.client.ModKeybinds;
import net.fabric.grabthat.config.PickupConfig;

@Mod(value = GrabThat.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GrabThat.MODID, value = Dist.CLIENT)
public class GrabThatClient {
    public GrabThatClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        PickupConfig.init();

        CarryClientEvents.register();
        CarryRenderer.register();
        CarryPlayerEvents.register();
        CarryHud.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.GRAB_KEY);
    }
}
