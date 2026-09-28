package net.forge.grabthat;

import net.forge.grabthat.client.GrabThatClient;
import net.forge.grabthat.config.CarryHeights;
import net.forge.grabthat.event.CarryEvents;
import net.forge.grabthat.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GrabThat.MODID)
public class GrabThat {
    public static final String MODID = "grabthat";

    public GrabThat() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(ModNetwork::commonSetup);
        modEventBus.addListener(GrabThatClient::init);

        MinecraftForge.EVENT_BUS.addListener(CarryEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(CarryEvents::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(CarryEvents::onPlayerLogout);
        MinecraftForge.EVENT_BUS.addListener(CarryEvents::onEntityMount);
        MinecraftForge.EVENT_BUS.addListener(CarryHeights::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(CarryHeights::onRegisterCommands);
    }
}