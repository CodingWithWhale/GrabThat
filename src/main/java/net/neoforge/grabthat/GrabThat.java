package net.neoforge.grabthat;

import net.neoforge.grabthat.config.CarryHeights;
import net.neoforge.grabthat.config.UnpickupableMobs;
import net.neoforge.grabthat.event.CarryEvents;
import net.neoforge.grabthat.network.ModNetwork;
import net.neoforge.grabthat.registry.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(GrabThat.MODID)
public class GrabThat {
    public static final String MODID = "grabthat";

    public GrabThat(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(ModNetwork::registerPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(CarryEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(CarryEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(CarryHeights::onServerStarted);
        NeoForge.EVENT_BUS.addListener(CarryHeights::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(UnpickupableMobs::onServerStarted);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}