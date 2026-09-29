package net.forge.grabthat.client;

import net.forge.grabthat.config.PickupConfig;
import net.forge.grabthat.data.CarryData;
import net.forge.grabthat.data.CarryType;
import net.forge.grabthat.network.DismountSelfPacket;
import net.forge.grabthat.network.DropPacket;
import net.forge.grabthat.network.ModNetwork;
import net.forge.grabthat.network.PickupBlockPacket;
import net.forge.grabthat.network.PickupEntityPacket;
import net.forge.grabthat.network.ThrowPacket;
import net.forge.grabthat.util.CarryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;

public class CarryClientEvents {

    private static boolean prevShiftDown = false;
    private static boolean useKeyDown = false;
    private static Entity frozenClientEntity = null;
    private static boolean grabKeyDown = false;

    private CarryClientEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(CarryClientEvents.class);
    }

    @SubscribeEvent
    public static void onInteractionTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        LocalPlayer player = mc.player;
        var data = CarryClientContext.get(player.getUUID());

        if (data.isEmpty()) return;

        if (event.isUseItem()) {
            if (ModKeybinds.GRAB_KEY.isDown()) {
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

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
                    ModNetwork.sendToServer(new ThrowPacket(power));
                } else {
                    CarryClientContext.set(player.getUUID(), latest.withAiming(false));
                }
            }
        }
        useKeyDown = useDown;

        repositionAllCarried(mc);

        boolean shiftDown = isShiftDown(mc);
        boolean ridingCarried = player.isPassenger()
                && player.getVehicle() instanceof Player;
        if (ridingCarried && prevShiftDown && !shiftDown) {
            ModNetwork.sendToServer(new DismountSelfPacket());
        }
        prevShiftDown = shiftDown;

        int wantedId = data.isEmpty() ? -1 : data.entityId();
        Entity prev = frozenClientEntity;
        if (prev != null && (wantedId == -1 || prev.getId() != wantedId)) {
            prev.noPhysics = false;
            frozenClientEntity = null;
        }
        if (wantedId != -1 && frozenClientEntity == null) {
            Entity carried = mc.level.getEntity(wantedId);
            if (carried != null && carried.isAlive()) {
                carried.noPhysics = true;
                carried.noCulling = true;
                frozenClientEntity = carried;
            }
        }

        if (!data.aiming()) {
            if (data.carryType() == CarryType.MOB || data.carryType() == CarryType.PLAYER) {
                Entity carried = mc.level.getEntity(data.entityId());
                if (carried instanceof LivingEntity living) {
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
            Entity carried = mc.level.getEntity(myData.entityId());
            if (carried != null && carried != self && carried.getVehicle() != self) {
                carried.startRiding(self, true);
            }
        } else {
            for (Entity e : self.getPassengers()) {
                if (e instanceof Player && e.getVehicle() == self) {
                    e.stopRiding();
                }
            }
        }

        for (Player other : mc.level.players()) {
            if (other == self) continue;
            CarryData d = CarryClientContext.get(other.getUUID());
            boolean carriesMe = d.carryType() == CarryType.PLAYER && d.entityId() == self.getId();
            if (carriesMe) {
                if (self.getVehicle() != other) self.startRiding(other, true);
            } else if (self.getVehicle() == other) {
                self.stopRiding();
            }

            if (d.carryType() == CarryType.PLAYER) {
                Entity carried = mc.level.getEntity(d.entityId());
                if (carried != null && carried != self && carried.getVehicle() != other) {
                    carried.startRiding(other, true);
                }
            } else {
                for (Entity e : other.getPassengers()) {
                    if (e instanceof Player && e.getVehicle() == other) {
                        e.stopRiding();
                    }
                }
            }
        }
    }

    private static void repositionAllCarried(Minecraft mc) {
        for (Player carrier : mc.level.players()) {
            CarryData d = CarryClientContext.get(carrier.getUUID());
            if (d.isEmpty()) continue;

            Entity carried = mc.level.getEntity(d.entityId());
            if (carried == null || !carried.isAlive()) continue;

            if (d.carryType() == CarryType.PLAYER && carried.getVehicle() != carrier) continue;

            carried.noCulling = true;
            carried.setDeltaMovement(0, 0, 0);

            Vec3 seat;
            Vec3 prevSeat;
            if (d.carryType() == CarryType.PLAYER) {
                seat = new Vec3(carrier.getX(), carrier.getY() + CarryUtil.PLAYER_SEAT_Y, carrier.getZ());
                prevSeat = new Vec3(carrier.xo, carrier.yo + CarryUtil.PLAYER_SEAT_Y, carrier.zo);
            } else if (carried instanceof LivingEntity cargo) {
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
                    && carried instanceof LivingEntity living) {
                living.yBodyRotO = carrier.yBodyRotO;
                living.yBodyRot = carrier.yBodyRot;
                living.yHeadRotO = carrier.yBodyRotO;
                living.yHeadRot = carrier.yBodyRot;
                carried.yRotO = carrier.yBodyRotO;
                carried.setYRot(carrier.yBodyRot);
            }
        }
    }

    private static boolean isCarryableEntity(Entity entity) {
        return entity instanceof LivingEntity living
                && !(living instanceof Player)
                && !(living instanceof ArmorStand);
    }

    private static boolean isShiftDown(Minecraft mc) {
        return mc.options.keyShift.isDown();
    }

    private static void handleGrabKey(Minecraft mc) {
        boolean down = ModKeybinds.GRAB_KEY.isDown();
        if (down && !grabKeyDown && mc.screen == null) {
            if (PickupConfig.isEnabled()) {
                LocalPlayer player = mc.player;
                if (player != null && !CarryClientContext.get(player.getUUID()).isEmpty()) {
                    CarryClientContext.set(player.getUUID(), CarryData.EMPTY);
                    ModNetwork.sendToServer(new DropPacket());
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
            Entity target = entityHit.getEntity();
            carryIntent = target instanceof Player || isCarryableEntity(target);
        }
        if (!carryIntent) return;

        if (hit instanceof EntityHitResult entityHit) {
            ModNetwork.sendToServer(new PickupEntityPacket(entityHit.getEntity().getId()));
        } else if (hit instanceof BlockHitResult blockHit) {
            ModNetwork.sendToServer(new PickupBlockPacket(blockHit.getBlockPos()));
        }
    }
}