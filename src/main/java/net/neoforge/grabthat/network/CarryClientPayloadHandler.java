package net.neoforge.grabthat.network;

import net.neoforge.grabthat.client.CarryClientContext;
import net.neoforge.grabthat.data.CarryData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class CarryClientPayloadHandler {

    private CarryClientPayloadHandler() {}

    public static void handleCarrySync(CarrySyncPayload payload) {
        CarryClientContext.set(payload.playerUUID(), payload.data());
    }

    public static void handleCarriedLaunch(CarriedLaunchPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        LocalPlayer self = mc.player;

        CarryClientContext.set(payload.carrierUUID(), CarryData.EMPTY);

        if (self.getVehicle() instanceof net.minecraft.world.entity.player.Player) {
            self.stopRiding();
        }
        self.moveTo(payload.x(), payload.y(), payload.z(), self.getYRot(), self.getXRot());
        self.setDeltaMovement(payload.vx(), payload.vy(), payload.vz());
        self.hasImpulse = true;
    }
}