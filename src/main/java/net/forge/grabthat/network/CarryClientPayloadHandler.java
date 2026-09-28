package net.forge.grabthat.network;

import net.forge.grabthat.client.CarryClientContext;
import net.forge.grabthat.data.CarryData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class CarryClientPayloadHandler {

    private CarryClientPayloadHandler() {}

    public static void handleCarrySync(CarrySyncPacket msg) {
        CarryClientContext.set(msg.playerUUID(), msg.data());
    }

    public static void handleCarriedLaunch(CarriedLaunchPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        LocalPlayer self = mc.player;

        CarryClientContext.set(msg.carrierUUID(), CarryData.EMPTY);

        if (self.getVehicle() instanceof net.minecraft.world.entity.player.Player) {
            self.stopRiding();
        }
        self.moveTo(msg.x(), msg.y(), msg.z(), self.getYRot(), self.getXRot());
        self.setDeltaMovement(msg.vx(), msg.vy(), msg.vz());
        self.hasImpulse = true;
    }
}