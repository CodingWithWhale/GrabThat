package net.forge.grabthat.event;

import java.util.List;

import net.forge.grabthat.data.CarryData;
import net.forge.grabthat.data.CarryType;
import net.forge.grabthat.network.CarryPayloadHandler;
import net.forge.grabthat.network.ModNetwork;
import net.forge.grabthat.storage.CarryDataStorage;
import net.forge.grabthat.util.CarryDismountGuard;
import net.forge.grabthat.util.CarryUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CarryEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null) return;

        if (!CarryPayloadHandler.THROWN_REMAINING.isEmpty()) {
            var it = CarryPayloadHandler.THROWN_REMAINING.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                ServerPlayer thrown = null;
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p.getId() == entry.getKey()) {
                        thrown = p;
                        break;
                    }
                }
                if (thrown == null || !thrown.isAlive() || thrown.isRemoved() || thrown.onGround()) {
                    it.remove();
                    continue;
                }
                thrown.connection.send(new ClientboundSetEntityMotionPacket(thrown));
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) it.remove();
                else entry.setValue(remaining);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CarryData data = CarryDataStorage.get(player);
            if (data.isEmpty()) continue;

            if (data.carryType() == CarryType.MOB || data.carryType() == CarryType.PLAYER) {
                Entity carried = player.level().getEntity(data.entityId());
                if (carried == null || !carried.isAlive()) {
                    CarryDataStorage.set(player, CarryData.EMPTY);
                    ModNetwork.sendCarrySync(player);
                    continue;
                }

                if (data.carryType() == CarryType.PLAYER) {
                    if (carried.isPassenger() && carried.getVehicle() == player) {
                        carried.setPos(player.getX(), player.getY() + CarryUtil.PLAYER_SEAT_Y, player.getZ());
                        continue;
                    }
                    CarryPayloadHandler.unfreezeEntity(carried);
                    CarryDataStorage.set(player, CarryData.EMPTY);
                    ModNetwork.sendCarrySync(player);
                    continue;
                }

                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                if (carried instanceof LivingEntity cargo) {
                    Vec3 seat = CarryUtil.mobSeatPlacement(cargo, x, y, z, player.yBodyRot);
                    x = seat.x;
                    y = seat.y;
                    z = seat.z;
                } else {
                    y += CarryUtil.MOB_SEAT_Y;
                }
                float yaw = player.yBodyRot;

                carried.setPos(x, y, z);
                carried.setYRot(yaw);
                carried.setYHeadRot(yaw);
                if (carried instanceof LivingEntity living) living.yBodyRot = yaw;

                carried.setDeltaMovement(Vec3.ZERO);
                carried.noPhysics = true;
                carried.setNoGravity(true);
                if (carried instanceof Mob mob) mob.setNoAi(true);
                carried.setInvulnerable(true);
                carried.setSilent(true);

                if (carried instanceof LivingEntity living) {
                    double factor = CarryUtil.lightweightFallFactor(living);
                    if (factor < 0.95) {
                        Vec3 mv = player.getDeltaMovement();
                        if (mv.y < 0) {
                            player.setDeltaMovement(mv.x, mv.y * factor, mv.z);
                            player.fallDistance = 0.0F;
                        }
                    }
                }
            }

            if (data.carryType() == CarryType.BLOCK
                    && data.blockState() != null
                    && data.blockState().getBlock() instanceof AbstractFurnaceBlock) {
                tickCarriedFurnace(player, data);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!CarryDataStorage.get(player).isEmpty()) {
                CarryDataStorage.set(player, CarryData.EMPTY);
                ModNetwork.sendCarrySync(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CarryData data = CarryDataStorage.get(player);
            if (!data.isEmpty()) {
                if (data.carryType() == CarryType.MOB || data.carryType() == CarryType.PLAYER) {
                    Entity carried = player.level().getEntity(data.entityId());
                    if (carried != null && carried.isAlive()) {
                        carried.noPhysics = false;
                        carried.setNoGravity(false);
                        if (carried instanceof Mob mob) mob.setNoAi(false);
                        carried.setInvulnerable(false);
                        carried.setSilent(false);
                    }
                }
                CarryDataStorage.clear(player);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (!event.isDismounting()) return;
        if (event.getLevel().isClientSide) return;
        if (CarryDismountGuard.forced()) return;

        Entity mounting = event.getEntityMounting();
        Entity vehicle = event.getEntityBeingMounted();
        if (!(mounting instanceof Player) || !(vehicle instanceof ServerPlayer carrier)) return;

        if (!vehicle.isAlive() || vehicle.isRemoved()) return;

        CarryData data = CarryDataStorage.get(carrier);
        if (data.carryType() == CarryType.PLAYER && data.entityId() == mounting.getId()) {
            event.setCanceled(true);
            mounting.setShiftKeyDown(false);
        }
    }

    private static void tickCarriedFurnace(ServerPlayer player, CarryData data) {
        ServerLevel level = player.serverLevel();

        NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
        List<ItemStack> src = data.blockContents();
        for (int i = 0; i < Math.min(3, src.size()); i++) {
            items.set(i, src.get(i).copy());
        }

        SmeltingRecipe recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(items.get(0)), level)
                .orElse(null);

        boolean burning = data.litTime() > 0;
        if (burning) {
            data.setLitTime(data.litTime() - 1);
            if (recipe != null && canFitResult(level, items, recipe)) {
                if (data.cookTimeTotal() <= 0) {
                    data.setCookTimeTotal(recipe.getCookingTime());
                }
                data.setCookTime(data.cookTime() + 1);
                if (data.cookTime() >= data.cookTimeTotal()) {
                    ItemStack result = recipe.getResultItem(level.registryAccess());
                    items.get(0).shrink(1);
                    if (items.get(2).isEmpty()) {
                        items.set(2, result.copy());
                    } else {
                        items.get(2).grow(result.getCount());
                    }
                    data.setCookTime(0);
                    data.setCookTimeTotal(recipe.getCookingTime());
                }
            } else {
                data.setCookTime(0);
            }
        } else if (data.cookTime() > 0) {
            data.setCookTime(Math.max(0, data.cookTime() - 2));
        }

        if (data.litTime() <= 0 && recipe != null && canFitResult(level, items, recipe) && !items.get(1).isEmpty()) {
            int burn = items.get(1).getBurnTime(RecipeType.SMELTING);
            if (burn > 0) {
                data.setLitTime(burn);
                data.setCookTimeTotal(recipe.getCookingTime());
                items.get(1).shrink(1);
            }
        }

        data.setBlockContents(items);
    }

    private static boolean canFitResult(ServerLevel level, NonNullList<ItemStack> items, SmeltingRecipe recipe) {
        if (items.get(0).isEmpty()) return false;
        ItemStack out = items.get(2);
        if (out.isEmpty()) return true;
        ItemStack result = recipe.getResultItem(level.registryAccess());
        return ItemStack.isSameItemSameTags(out, result) && out.getCount() + result.getCount() <= 64;
    }
}