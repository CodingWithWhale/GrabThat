package net.fabric.grabthat.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.fabric.grabthat.data.CarryData;
import net.fabric.grabthat.data.CarryType;
import net.fabric.grabthat.registry.ModAttachments;
import net.fabric.grabthat.util.CarryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class CarryPayloadHandler {

    public static final java.util.Map<Integer, Integer> THROWN_REMAINING = new java.util.HashMap<>();

    public static final Set<EntityType<?>> UNCARRYABLE = Set.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.WARDEN,
            EntityType.ELDER_GUARDIAN,
            EntityType.GHAST
    );

    private CarryPayloadHandler() {}

    public static void handlePickupEntity(Player player, PickupEntityPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return;

        CarryData carry = serverPlayer.getData(ModAttachments.CARRY_DATA);
        if (!carry.isEmpty()) return;

        Entity target = serverPlayer.level().getEntity(payload.targetEntityId());
        if (target == null || !target.isAlive()) return;

        boolean isPlayer = target instanceof Player;
        boolean isMob = target instanceof LivingEntity && !(target instanceof Player)
                && !(target instanceof ArmorStand);

        if (!isPlayer && !isMob) return;

        if (UNCARRYABLE.contains(target.getType())) return;

        if (isPlayer) {
            Player other = (Player) target;

            if (serverPlayer.getVehicle() == other || other.getVehicle() == serverPlayer) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component
                        .literal("\u00a7cYou can't carry the player who is carrying you."), true);
                return;
            }
            CarryData otherData = other.getData(ModAttachments.CARRY_DATA);
            if (!otherData.isEmpty()) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component
                        .literal("\u00a7cThat player is already carrying something."), true);
                return;
            }

            if (other.isPassenger()) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component
                        .literal("\u00a7cThat player is already being carried."), true);
                return;
            }
        }

        if (serverPlayer.distanceToSqr(target) > 64.0) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component
                    .literal("\u00a7cToo far away to grab."), true);
            return;
        }

        if (target instanceof Mob mob && mob.isLeashed()) {
            mob.dropLeash(true, false);
        }

        freezeEntity(target);

        if (isPlayer) {

            target.startRiding(serverPlayer, true);
        }

        CarryData newData = new CarryData(isPlayer ? CarryType.PLAYER : CarryType.MOB,
                target.getId(), target.getUUID(), null, List.of(), null);
        serverPlayer.setData(ModAttachments.CARRY_DATA, newData);
        ModNetwork.sendCarrySync(serverPlayer);
    }

    public static void handlePickupBlock(Player player, PickupBlockPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return;

        CarryData carry = serverPlayer.getData(ModAttachments.CARRY_DATA);
        if (!carry.isEmpty()) return;

        BlockPos pos = payload.blockPos();
        if (!serverPlayer.level().mayInteract(serverPlayer, pos)) return;
        if (!inPickupRange(serverPlayer, pos)) return;

        BlockState worldState = serverPlayer.level().getBlockState(pos);
        if (!CarryUtil.isUtilityBlockWithPart(serverPlayer.level(), pos)) return;

        if (worldState.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) return;

        BlockState secondState = null;
        BlockPos secondPos = null;
        boolean chestDouble = worldState.getBlock() instanceof AbstractChestBlock;
        if (chestDouble) {
            ChestType type = worldState.hasProperty(ChestBlock.TYPE) ? worldState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
            if (type != ChestType.SINGLE) {
                BlockPos other = pos.relative(ChestBlock.getConnectedDirection(worldState));
                BlockState otherState = serverPlayer.level().getBlockState(other);
                if (otherState.getBlock() instanceof AbstractChestBlock && serverPlayer.level().getBlockEntity(other) instanceof Container) {
                    secondState = otherState;
                    secondPos = other;
                }
            }
        } else {
            secondPos = CarryUtil.findSecondPart(serverPlayer.level(), pos, worldState);
            if (secondPos != null) {
                secondState = serverPlayer.level().getBlockState(secondPos);
            }
        }

        if (secondPos != null && secondPos.getY() < pos.getY()) {
            BlockState tmpState = secondState;
            secondState = worldState;
            worldState = tmpState;
            BlockPos tmpPos = secondPos;
            secondPos = pos;
            pos = tmpPos;
        }

        BlockState state = worldState;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, serverPlayer.getDirection());
        }
        if (secondState != null && secondState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            secondState = secondState.setValue(BlockStateProperties.HORIZONTAL_FACING, serverPlayer.getDirection());
        } else if (secondState == null && state.getBlock() instanceof AbstractChestBlock
                && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {

            state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }

        List<ItemStack> contents = new ArrayList<>();
        net.minecraft.nbt.CompoundTag blockNbt = null;
        net.minecraft.nbt.CompoundTag secondBlockNbt = null;
        BlockEntity be = serverPlayer.level().getBlockEntity(pos);
        if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) {
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    contents.add(container.getItem(i).copy());
                }
            }
        } else if (chestDouble && secondPos != null) {
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    contents.add(container.getItem(i).copy());
                }
            }
            if (serverPlayer.level().getBlockEntity(secondPos) instanceof Container secondContainer) {
                for (int i = 0; i < secondContainer.getContainerSize(); i++) {
                    contents.add(secondContainer.getItem(i).copy());
                }
            }
        } else {
            if (be != null) {
                blockNbt = be.saveWithId(serverPlayer.level().registryAccess());
            }
            if (secondPos != null) {
                BlockEntity secondBe = serverPlayer.level().getBlockEntity(secondPos);
                if (secondBe != null) {
                    secondBlockNbt = secondBe.saveWithId(serverPlayer.level().registryAccess());
                }
            }
        }

        if (be != null) serverPlayer.level().removeBlockEntity(pos);
        serverPlayer.level().removeBlock(pos, false);
        if (secondPos != null) {
            if (serverPlayer.level().getBlockEntity(secondPos) != null) {
                serverPlayer.level().removeBlockEntity(secondPos);
            }
            serverPlayer.level().removeBlock(secondPos, false);
        }

        CarryData newData = new CarryData(CarryType.BLOCK, -1, null, state, secondState, contents, pos, secondPos, blockNbt, secondBlockNbt);

        if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) {
            net.minecraft.nbt.CompoundTag tag = be.saveWithFullMetadata(serverPlayer.level().registryAccess());
            newData.setLitTime(tag.getInt("BurnTime"));
            newData.setCookTime(tag.getInt("CookTime"));
            newData.setCookTimeTotal(tag.getInt("CookTimeTotal"));
        }

        serverPlayer.setData(ModAttachments.CARRY_DATA, newData);
        ModNetwork.sendCarrySync(serverPlayer);
    }

    public static void handleThrow(Player player, ThrowPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        CarryData carry = serverPlayer.getData(ModAttachments.CARRY_DATA);
        if (carry.isEmpty()) return;

        float power = Math.max(0f, Math.min(1f, payload.power()));

        if (carry.carryType() == CarryType.BLOCK) {
            placeBlock(serverPlayer, carry);
        } else {
            throwEntity(serverPlayer, carry, power);
        }

        serverPlayer.setData(ModAttachments.CARRY_DATA, CarryData.EMPTY);
        ModNetwork.sendCarrySync(serverPlayer);
    }

    public static void handleDrop(Player player, DropPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        CarryData carry = serverPlayer.getData(ModAttachments.CARRY_DATA);
        if (carry.isEmpty()) return;

        if (carry.carryType() == CarryType.BLOCK) {
            placeBlock(serverPlayer, carry);
        } else {
            releaseEntity(serverPlayer, carry);
        }

        serverPlayer.setData(ModAttachments.CARRY_DATA, CarryData.EMPTY);
        ModNetwork.sendCarrySync(serverPlayer);
    }

    public static void handleDismountSelf(Player player, DismountSelfPayload payload) {
        if (!(player instanceof ServerPlayer self)) return;

        ServerPlayer carrier = null;
        for (ServerPlayer p : self.server.getPlayerList().getPlayers()) {
            CarryData data = p.getData(ModAttachments.CARRY_DATA);
            if (data.carryType() == CarryType.PLAYER && data.entityId() == self.getId()) {
                carrier = p;
                break;
            }
        }

        if (carrier != null) {
            ejectEntity(self);
            unfreezeEntity(self);
            self.teleportTo(carrier.getX(), carrier.getY() + 0.5, carrier.getZ());
            carrier.setData(ModAttachments.CARRY_DATA, CarryData.EMPTY);
            ModNetwork.sendCarrySync(carrier);
        }
    }

    private static void placeBlock(ServerPlayer player, CarryData carry) {
        BlockState state = carry.blockState();
        if (state == null) return;

        BlockPos placePos = null;
        net.minecraft.world.phys.HitResult hit = player.pick(4.5, 0.0F, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
            BlockPos placed = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (player.level().isEmptyBlock(placed)) {
                placePos = placed;
            }
        }
        if (placePos == null) {
            placePos = player.blockPosition().relative(player.getDirection(), 2);
            for (int attempt = 0; attempt < 3; attempt++) {
                if (player.level().isEmptyBlock(placePos)) break;
                placePos = placePos.above();
            }
        }
        if (placePos == null || !player.level().isEmptyBlock(placePos)) return;

        BlockState placeState = state;
        if (placeState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            placeState = placeState.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    player.getDirection().getOpposite());
        }

        BlockState secondState = carry.secondBlockState();
        boolean doubleChest = secondState != null
                && placeState.getBlock() instanceof AbstractChestBlock
                && placeState.hasProperty(ChestBlock.TYPE)
                && placeState.getValue(ChestBlock.TYPE) != ChestType.SINGLE;

        if (doubleChest) {
            Direction pairDir = ChestBlock.getConnectedDirection(placeState);
            BlockPos secondPos = placePos.relative(pairDir);
            if (!player.level().isEmptyBlock(secondPos)) return;
            BlockState secondPlaceState = placeState.setValue(ChestBlock.TYPE,
                    placeState.getValue(ChestBlock.TYPE).getOpposite());

            player.level().setBlock(placePos, placeState, 2);
            player.level().setBlock(secondPos, secondPlaceState, 2);

            List<ItemStack> contents = carry.blockContents();
            BlockEntity primaryBe = player.level().getBlockEntity(placePos);
            BlockEntity secondBe = player.level().getBlockEntity(secondPos);
            if (primaryBe instanceof Container primaryContainer) {
                int slots = Math.min(primaryContainer.getContainerSize(), contents.size());
                for (int i = 0; i < slots; i++) {
                    primaryContainer.setItem(i, contents.get(i).copy());
                }
                primaryBe.setChanged();
            }
            if (secondBe instanceof Container secondContainer) {
                int offset = primaryBe instanceof Container primaryContainer2
                        ? primaryContainer2.getContainerSize() : 27;
                for (int i = 0; i < secondContainer.getContainerSize() && offset + i < contents.size(); i++) {
                    secondContainer.setItem(i, contents.get(offset + i).copy());
                }
                secondBe.setChanged();
            }
            return;
        }

        if (secondState != null) {
            BlockState secondPlaceState = secondState;
            if (placeState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    && secondPlaceState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                secondPlaceState = secondPlaceState.setValue(BlockStateProperties.HORIZONTAL_FACING,
                        player.getDirection().getOpposite());
            }
            BlockPos secondPlacePos = null;
            BlockPos offset = CarryUtil.secondPartOffset(placeState, carry.blockPos(), carry.secondBlockPos());
            if (offset != null) {
                secondPlacePos = placePos.offset(offset);
            }
            if (secondPlacePos == null || !player.level().isEmptyBlock(secondPlacePos)) return;

            boolean primaryIsBase = true;
            if (placeState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                primaryIsBase = placeState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
            } else if (placeState.hasProperty(net.minecraft.world.level.block.BedBlock.PART)) {
                primaryIsBase = placeState.getValue(net.minecraft.world.level.block.BedBlock.PART)
                        == net.minecraft.world.level.block.state.properties.BedPart.FOOT;
            }

            BlockPos basePos = primaryIsBase ? placePos : secondPlacePos;
            BlockState baseState = primaryIsBase ? placeState : secondPlaceState;
            BlockPos topPos = primaryIsBase ? secondPlacePos : placePos;
            BlockState topState = primaryIsBase ? secondPlaceState : placeState;

            player.level().setBlockAndUpdate(basePos, baseState);
            player.level().setBlockAndUpdate(topPos, topState);

            BlockEntity be = player.level().getBlockEntity(placePos);
            if (carry.blockNbt() != null && be != null) {
                be.loadWithComponents(carry.blockNbt(), player.level().registryAccess());
                be.setChanged();
            }
            BlockEntity secondBe = player.level().getBlockEntity(secondPlacePos);
            if (carry.secondBlockNbt() != null && secondBe != null) {
                secondBe.loadWithComponents(carry.secondBlockNbt(), player.level().registryAccess());
                secondBe.setChanged();
            }
            return;
        }

        player.level().setBlockAndUpdate(placePos, placeState);
        BlockEntity be = player.level().getBlockEntity(placePos);
        if (carry.blockNbt() != null && be != null) {

            be.loadWithComponents(carry.blockNbt(), player.level().registryAccess());
            be.setChanged();
        } else if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {

            net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
            List<ItemStack> src = carry.blockContents();
            for (int i = 0; i < Math.min(3, src.size()); i++) items.set(i, src.get(i).copy());
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            net.minecraft.world.ContainerHelper.saveAllItems(tag, items, player.level().registryAccess());
            tag.putInt("BurnTime", carry.litTime());
            tag.putInt("CookTime", carry.cookTime());
            tag.putInt("CookTimeTotal", carry.cookTimeTotal());
            furnace.loadWithComponents(tag, player.level().registryAccess());
            furnace.setChanged();
            boolean litNow = carry.litTime() > 0;
            if (placeState.hasProperty(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)
                    && placeState.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT) != litNow) {
                player.level().setBlockAndUpdate(placePos, placeState.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, litNow));
            }
        } else if (be instanceof Container container) {
            int slots = Math.min(container.getContainerSize(), carry.blockContents().size());
            for (int i = 0; i < slots; i++) {
                container.setItem(i, carry.blockContents().get(i).copy());
            }
            be.setChanged();
        }
    }

    private static boolean inPickupRange(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    private static void throwEntity(ServerPlayer player, CarryData carry, float power) {
        Entity carried = player.level().getEntity(carry.entityId());
        if (carried == null) return;

        ejectEntity(carried);
        unfreezeEntity(carried);

        double launchX = player.getX();
        double launchY = player.getY();
        double launchZ = player.getZ();
        if (carried instanceof Player carriedPlayer) {
            carriedPlayer.teleportTo(launchX, launchY, launchZ);
        }

        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 flatLook = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();
        double strength = 0.6 + power * 2.6;
        double vx = flatLook.x * strength;
        double vy = 0.45 + power * 0.45;
        double vz = flatLook.z * strength;
        carried.setDeltaMovement(vx, vy, vz);
        carried.hurtMarked = true;
        carried.hasImpulse = true;
        carried.fallDistance = 0f;

        if (carried instanceof ServerPlayer carriedPlayer) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(carriedPlayer,
                    new CarriedLaunchPayload(player.getUUID(),
                            launchX, launchY, launchZ,
                            vx, vy, vz));
        }

        THROWN_REMAINING.put(carried.getId(), 15);
    }

    private static void releaseEntity(ServerPlayer player, CarryData carry) {
        Entity carried = player.level().getEntity(carry.entityId());
        if (carried == null) return;
        ejectEntity(carried);
        unfreezeEntity(carried);
        if (carried instanceof Player carriedPlayer) {
            carriedPlayer.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
        }
    }

    private static void ejectEntity(Entity entity) {
        try {
            net.fabric.grabthat.util.CarryDismountGuard.force();
            entity.stopRiding();
        } finally {
            net.fabric.grabthat.util.CarryDismountGuard.release();
        }
    }

    private static void freezeEntity(Entity entity) {
        entity.setNoGravity(true);
        entity.noPhysics = true;

        entity.noCulling = true;
        if (entity instanceof Mob mob) mob.setNoAi(true);
        entity.setInvulnerable(true);
        if (entity instanceof LivingEntity living) {
            living.setSilent(true);
        }
    }

    public static void unfreezeEntity(Entity entity) {
        entity.setNoGravity(false);
        entity.noPhysics = false;
        entity.noCulling = false;
        if (entity instanceof Mob mob) mob.setNoAi(false);
        entity.setInvulnerable(false);
        if (entity instanceof LivingEntity living) {
            living.setSilent(false);
        }
    }
}