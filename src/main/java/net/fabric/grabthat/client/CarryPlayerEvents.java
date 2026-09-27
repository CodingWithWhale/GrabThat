package net.fabric.grabthat.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;

public class CarryPlayerEvents {

    private CarryPlayerEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(CarryPlayerEvents.class);
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        var data = CarryClientContext.get(player.getUUID());
        if (data.isEmpty()) return;
        if (player.isDeadOrDying() || player.isSleeping()) return;

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
        model.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
    }
}