package net.forge.grabthat.util;

import java.util.Set;

import net.forge.grabthat.config.CarryHeights;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public final class CarryUtil {
    private CarryUtil() {}

    public static final double PLAYER_SEAT_Y = 1.2;

    public static final double MOB_SEAT_Y = 2.0;

    public static final double QUADRUPED_SEAT_Y = 3.5;

    public static final Set<Block> UTILITY_BLOCKS = Set.of(
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.BARREL,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.ENDER_CHEST,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.HOPPER,
            Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.GRINDSTONE,
            Blocks.SMITHING_TABLE,
            Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.FLETCHING_TABLE,
            Blocks.LECTERN,
            Blocks.STONECUTTER,
            Blocks.JUKEBOX,
            Blocks.DECORATED_POT,
            Blocks.COMPOSTER,
            Blocks.BELL,
            Blocks.RESPAWN_ANCHOR,
            Blocks.BEACON,
            Blocks.CONDUIT,
            Blocks.MAGMA_BLOCK
    );

    public static boolean isUtilityBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (UTILITY_BLOCKS.contains(state.getBlock())) return true;

        return state.hasBlockEntity();
    }

    public static boolean isUtilityBlockWithPart(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (isUtilityBlock(state)) return true;
        BlockPos second = findSecondPart(level, pos, state);
        return second != null && isUtilityBlock(level.getBlockState(second));
    }

    public static BlockPos findSecondPart(BlockGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) return null;
        Block block = state.getBlock();

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos other = half == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
            BlockState otherState = level.getBlockState(other);
            if (otherState.getBlock() == block
                    && otherState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                return other;
            }
        }

        if (state.hasProperty(BedBlock.PART)) {
            BedPart part = state.getValue(BedBlock.PART);
            BlockPos other = pos.relative(BedBlock.getConnectedDirection(state));
            BlockState otherState = level.getBlockState(other);
            if (otherState.getBlock() == block
                    && otherState.hasProperty(BedBlock.PART)
                    && otherState.getValue(BedBlock.PART) != part) {
                return other;
            }
            return null;
        }

        BlockPos above = pos.above();
        BlockPos below = pos.below();
        boolean aboveSame = level.getBlockState(above).getBlock() == block;
        boolean belowSame = level.getBlockState(below).getBlock() == block;
        if (aboveSame && !belowSame && hasExactlyOneBlockEntity(level, pos, above)) return above;
        if (belowSame && !aboveSame && hasExactlyOneBlockEntity(level, pos, below)) return below;
        return null;
    }

    private static boolean hasExactlyOneBlockEntity(BlockGetter level, BlockPos a, BlockPos b) {
        return (level.getBlockEntity(a) != null) != (level.getBlockEntity(b) != null);
    }

    public static BlockPos secondPartOffset(BlockState primary, BlockPos primaryPos, BlockPos secondPos) {
        if (primary != null && primary.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            boolean upper = primary.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
            return upper ? new BlockPos(0, -1, 0) : new BlockPos(0, 1, 0);
        }
        if (primary != null && primary.hasProperty(BedBlock.PART)) {
            Direction facing = primary.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? primary.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
            boolean foot = primary.getValue(BedBlock.PART) == BedPart.FOOT;
            Direction offsetDir = foot ? facing : facing.getOpposite();
            return new BlockPos(offsetDir.getStepX(), 0, offsetDir.getStepZ());
        }
        if (primaryPos != null && secondPos != null) {
            return secondPos.subtract(primaryPos);
        }
        return null;
    }

    public static double lightweightFallFactor(LivingEntity living) {
        if (living == null) return 1.0;
        if (living instanceof net.minecraft.world.entity.animal.Chicken) return 0.5;
        return 1.0;
    }

    public static boolean isUprightMob(LivingEntity living) {
        if (living == null) return false;
        return living instanceof net.minecraft.world.entity.npc.AbstractVillager
                || living instanceof net.minecraft.world.entity.monster.AbstractIllager
                || living instanceof net.minecraft.world.entity.monster.AbstractSkeleton
                || living instanceof net.minecraft.world.entity.monster.Zombie
                || living instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin
                || living instanceof net.minecraft.world.entity.animal.IronGolem
                || living instanceof net.minecraft.world.entity.monster.EnderMan
                || living instanceof net.minecraft.world.entity.monster.Giant
                || living instanceof net.minecraft.world.entity.boss.wither.WitherBoss;
    }

    public static double mobSeatOffset(LivingEntity carried) {
        Double override = CarryHeights.get(carried.getType());
        return override != null ? override : defaultSeat(carried);
    }

    public static double defaultSeat(LivingEntity living) {
        if (living instanceof net.minecraft.world.entity.animal.Chicken) return MOB_SEAT_Y;
        return isUprightMob(living) ? MOB_SEAT_Y : QUADRUPED_SEAT_Y;
    }

    public static final double UPRIGHT_FORWARD_PUSH = 1.0;

    public static Vec3 mobSeatPlacement(LivingEntity carried, double baseX, double baseY,
                                        double baseZ, float yaw) {
        double x = baseX;
        double z = baseZ;
        if (isUprightMob(carried)) {
            x += Mth.sin(yaw * Mth.DEG_TO_RAD) * UPRIGHT_FORWARD_PUSH;
            z -= Mth.cos(yaw * Mth.DEG_TO_RAD) * UPRIGHT_FORWARD_PUSH;
        }
        return new Vec3(x, baseY + mobSeatOffset(carried), z);
    }
}