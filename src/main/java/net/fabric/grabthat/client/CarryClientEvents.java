package net.fabric.grabthat.client;

import net.fabric.grabthat.data.CarryData;
import net.fabric.grabthat.data.CarryType;
import net.fabric.grabthat.network.DropPayload;
import net.fabric.grabthat.network.PickupBlockPayload;
import net.fabric.grabthat.network.PickupEntityPayload;
import net.fabric.grabthat.network.ThrowPayload;
import net.fabric.grabthat.config.PickupConfig;
import net.fabric.grabthat.util.CarryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public class CarryClientEvents {

    private static boolean prevShiftDown = false;
    private static boolean useKeyDown = false;
    private static net.minecraft.world.entity.Entity frozenClientEntity = null;

    private CarryClientEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(CarryClientEvents.class);
    }
    @SubscribeEvent
    public static void onInteractionTriggered(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        LocalPlayer player = mc.player;
        var data = CarryClientContext.get(player.getUUID());

        if (data.isEmpty()) return;

        if (event.isUseItem()) {
            if (isShiftDown(mc)) {
                player.connection.send(new DropPayload());
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }

            boolean startsAiming = (data.carryType() == CarryType.MOB || data.carryType() == CarryType.PLAYER)
                    && !data.aiming();
            if (data.aiming() || startsAiming) {
                if (startsAiming) {
                    CarryClientContext.set(player.getUUID(), data.withAiming(true));
                }
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        LocalPlayer player = mc.player;
        var data = CarryClientContext.get(player.getUUID());

        handleGrabKey(mc);
        syncClientRides(mc);

        boolean useDown = mc.options.keyUse.isDown();
        if (useKeyDown && !useDown) {
            var latest = CarryClientContext.get(player.getUUID());
            if (latest.aiming()) {
                if (!latest.isEmpty()) {
                    float power = latest.throwPower();

                    CarryClientContext.set(player.getUUID(), CarryData.EMPTY);
                    player.connection.send(new ThrowPayload(power));
                } else {
                    CarryClientContext.set(player.getUUID(), latest.withAiming(false));
                }
            }
        }
        useKeyDown = useDown;

        repositionAllCarried(mc);

        boolean shiftDown = isShiftDown(mc);
        boolean ridingCarried = player.isPassenger()
                && player.getVehicle() instanceof net.minecraft.world.entity.player.Player;
        if (ridingCarried && prevShiftDown && !shiftDown) {
            player.connection.send(new net.fabric.grabthat.network.DismountSelfPayload());
        }
        prevShiftDown = shiftDown;

        int wantedId = data.isEmpty() ? -1 : data.entityId();
        net.minecraft.world.entity.Entity prev = frozenClientEntity;
        if (prev != null && (wantedId == -1 || prev.getId() != wantedId)) {
            prev.noPhysics = false;
            frozenClientEntity = null;
        }
        if (wantedId != -1 && frozenClientEntity == null) {
            net.minecraft.world.entity.Entity carried = mc.level.getEntity(wantedId);
            if (carried != null && carried.isAlive()) {
                carried.noPhysics = true;
                carried.noCulling = true;
                frozenClientEntity = carried;
            }
        }

        if (!data.aiming()) {

            if (data.carryType() == CarryType.MOB || data.carryType() == CarryType.PLAYER) {
                net.minecraft.world.entity.Entity carried = mc.level.getEntity(data.entityId());
                if (carried instanceof net.minecraft.world.entity.LivingEntity living) {
                    double factor = CarryUtil.lightweightFallFactor(living);
                    if (factor < 0.95 && player.getDeltaMovement().y < 0) {
                        player.setDeltaMovement(player.getDeltaMovement().multiply(1, factor, 1));
                        player.fallDistance = 0.0F;
                    }
                }
            }

            return;
        }

        float speed = 0.04f;
        float power = data.throwPower() + speed * data.oscillationDirection();

        if (power >= 1f) {
            CarryClientContext.set(player.getUUID(), data.withThrowPower(1f).withOscillationDirection(-1f));
        } else if (power <= 0f) {
            CarryClientContext.set(player.getUUID(), data.withThrowPower(0f).withOscillationDirection(1f));
        } else {
            CarryClientContext.set(player.getUUID(), data.withThrowPower(power));
        }
    }

    private static void syncClientRides(Minecraft mc) {
        LocalPlayer self = mc.player;
        if (self == null || mc.level == null) return;

        var myData = CarryClientContext.get(self.getUUID());
        if (myData.carryType() == CarryType.PLAYER) {
            net.minecraft.world.entity.Entity carried = mc.level.getEntity(myData.entityId());
            if (carried != null && carried != self && carried.getVehicle() != self) {
                carried.startRiding(self, true);
            }
        } else {
            for (net.minecraft.world.entity.Entity e : self.getPassengers()) {
                if (e instanceof net.minecraft.world.entity.player.Player && e.getVehicle() == self) {
                    e.stopRiding();
                }
            }
        }

        for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
            if (other == self) continue;
            CarryData d = CarryClientContext.get(other.getUUID());
            boolean carriesMe = d.carryType() == CarryType.PLAYER && d.entityId() == self.getId();
            if (carriesMe) {
                if (self.getVehicle() != other) self.startRiding(other, true);
            } else if (self.getVehicle() == other) {
                self.stopRiding();
            }

            if (d.carryType() == CarryType.PLAYER) {
                net.minecraft.world.entity.Entity carried = mc.level.getEntity(d.entityId());
                if (carried != null && carried != self && carried.getVehicle() != other) {
                    carried.startRiding(other, true);
                }
            } else {
                for (net.minecraft.world.entity.Entity e : other.getPassengers()) {
                    if (e instanceof net.minecraft.world.entity.player.Player && e.getVehicle() == other) {
                        e.stopRiding();
                    }
                }
            }
        }
    }

    private static void repositionAllCarried(Minecraft mc) {
        for (net.minecraft.world.entity.player.Player carrier : mc.level.players()) {
            CarryData d = CarryClientContext.get(carrier.getUUID());
            if (d.isEmpty()) continue;

            net.minecraft.world.entity.Entity carried = mc.level.getEntity(d.entityId());
            if (carried == null || !carried.isAlive()) continue;

            if (d.carryType() == CarryType.PLAYER && carried.getVehicle() != carrier) continue;

            carried.noCulling = true;
            carried.setDeltaMovement(0, 0, 0);

            Vec3 seat;
            Vec3 prevSeat;
            if (d.carryType() == CarryType.PLAYER) {
                seat = new Vec3(carrier.getX(), carrier.getY() + CarryUtil.PLAYER_SEAT_Y, carrier.getZ());
                prevSeat = new Vec3(carrier.xo, carrier.yo + CarryUtil.PLAYER_SEAT_Y, carrier.zo);
            } else if (carried instanceof net.minecraft.world.entity.LivingEntity cargo) {
                seat = CarryUtil.mobSeatPlacement(cargo, carrier.getX(), carrier.getY(),
                        carrier.getZ(), carrier.yBodyRot);
                prevSeat = CarryUtil.mobSeatPlacement(cargo, carrier.xo, carrier.yo,
                        carrier.zo, carrier.yBodyRotO);
            } else {
                continue;
            }

            carried.xo = prevSeat.x;
            carried.yo = prevSeat.y;
            carried.zo = prevSeat.z;
            carried.xOld = prevSeat.x;
            carried.yOld = prevSeat.y;
            carried.zOld = prevSeat.z;
            carried.setPos(seat.x, seat.y, seat.z);

            if (d.carryType() == CarryType.MOB
                    && carried instanceof net.minecraft.world.entity.LivingEntity living) {
                living.yBodyRotO = carrier.yBodyRotO;
                living.yBodyRot = carrier.yBodyRot;
                living.yHeadRotO = carrier.yBodyRotO;
                living.yHeadRot = carrier.yBodyRot;
                carried.yRotO = carrier.yBodyRotO;
                carried.setYRot(carrier.yBodyRot);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event) {
        if (!CarryClientContext.get(event.getEntity().getUUID()).isEmpty()) {
            var model = event.getRenderer().getModel();
            model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS;
            model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS;
        }
    }

    private static boolean isCarryableEntity(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity living
                && !(living instanceof net.minecraft.world.entity.player.Player)
                && !(living instanceof net.minecraft.world.entity.decoration.ArmorStand);
    }

    private static boolean isShiftDown(Minecraft mc) {
        return mc.options.keyShift.isDown();
    }

    private static boolean grabKeyDown = false;

    private static void handleGrabKey(Minecraft mc) {
        boolean down = ModKeybinds.GRAB_KEY.isDown();
        if (down && !grabKeyDown && mc.screen == null) {
            if (PickupConfig.isEnabled()) {
                LocalPlayer player = mc.player;
                if (player != null && !CarryClientContext.get(player.getUUID()).isEmpty()) {
                    CarryClientContext.set(player.getUUID(), CarryData.EMPTY);
                    player.connection.send(new DropPayload());
                } else {
                    tryGrab(mc);
                }
            }
        }
        grabKeyDown = down;
    }

    private static void tryGrab(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        var data = CarryClientContext.get(player.getUUID());
        if (!data.isEmpty()) return;

        HitResult hit = mc.hitResult;
        boolean carryIntent = false;
        if (hit instanceof BlockHitResult blockHit) {
            carryIntent = CarryUtil.isUtilityBlockWithPart(mc.level, blockHit.getBlockPos());
        } else if (hit instanceof EntityHitResult entityHit) {
            net.minecraft.world.entity.Entity target = entityHit.getEntity();
            carryIntent = target instanceof net.minecraft.world.entity.player.Player || isCarryableEntity(target);
        }
        if (!carryIntent) return;

        if (hit instanceof EntityHitResult entityHit) {
            player.connection.send(new PickupEntityPayload(entityHit.getEntity().getId()));
        } else if (hit instanceof BlockHitResult blockHit) {
            player.connection.send(new PickupBlockPayload(blockHit.getBlockPos()));
        }
    }
}