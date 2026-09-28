package net.forge.grabthat.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CarryData {
    public static final CarryData EMPTY = new CarryData(CarryType.NONE, -1, null, null, null, List.of(), null, null, null, null);

    private final CarryType carryType;
    private final int entityId;
    private final UUID entityUUID;
    private final BlockState blockState;
    private final BlockState secondBlockState;
    private final CompoundTag blockNbt;
    private final CompoundTag secondBlockNbt;
    private List<ItemStack> blockContents;
    private final BlockPos blockPos;
    private final BlockPos secondBlockPos;
    private float throwPower;
    private boolean aiming;
    private float oscillationDirection;
    private int litTime;
    private int cookTime;
    private int cookTimeTotal;

    public CarryData(CarryType carryType, int entityId, UUID entityUUID, BlockState blockState, List<ItemStack> blockContents, BlockPos blockPos) {
        this(carryType, entityId, entityUUID, blockState, null, blockContents, blockPos, null, null, null);
    }

    public CarryData(CarryType carryType, int entityId, UUID entityUUID, BlockState blockState, BlockState secondBlockState, List<ItemStack> blockContents, BlockPos blockPos, CompoundTag blockNbt) {
        this(carryType, entityId, entityUUID, blockState, secondBlockState, blockContents, blockPos, null, blockNbt, null);
    }

    public CarryData(CarryType carryType, int entityId, UUID entityUUID, BlockState blockState, BlockState secondBlockState, List<ItemStack> blockContents, BlockPos blockPos, BlockPos secondBlockPos, CompoundTag blockNbt, CompoundTag secondBlockNbt) {
        this.carryType = carryType;
        this.entityId = entityId;
        this.entityUUID = entityUUID;
        this.blockState = blockState;
        this.secondBlockState = secondBlockState;
        this.blockNbt = blockNbt;
        this.secondBlockNbt = secondBlockNbt;
        this.blockContents = blockContents != null ? List.copyOf(blockContents) : List.of();
        this.blockPos = blockPos;
        this.secondBlockPos = secondBlockPos;
        this.throwPower = 0f;
        this.aiming = false;
        this.oscillationDirection = 1f;
    }

    public static void write(FriendlyByteBuf buf, CarryData data) {
        CarryType.write(buf, data.carryType);
        buf.writeInt(data.entityId);
        buf.writeBoolean(data.entityUUID != null);
        if (data.entityUUID != null) buf.writeUUID(data.entityUUID);
        buf.writeBoolean(data.blockState != null);
        if (data.blockState != null) buf.writeVarInt(Block.getId(data.blockState));
        buf.writeBoolean(data.secondBlockState != null);
        if (data.secondBlockState != null) buf.writeVarInt(Block.getId(data.secondBlockState));
        buf.writeVarInt(data.blockContents.size());
        for (ItemStack stack : data.blockContents) {
            buf.writeItemStack(stack, true);
        }
        buf.writeBoolean(data.blockPos != null);
        if (data.blockPos != null) buf.writeBlockPos(data.blockPos);
        buf.writeBoolean(data.secondBlockPos != null);
        if (data.secondBlockPos != null) buf.writeBlockPos(data.secondBlockPos);
        buf.writeBoolean(data.blockNbt != null);
        if (data.blockNbt != null) buf.writeNbt(data.blockNbt);
        buf.writeBoolean(data.secondBlockNbt != null);
        if (data.secondBlockNbt != null) buf.writeNbt(data.secondBlockNbt);
    }

    public static CarryData read(FriendlyByteBuf buf) {
        CarryType type = CarryType.read(buf);
        int entityId = buf.readInt();
        boolean hasUUID = buf.readBoolean();
        UUID uuid = hasUUID ? buf.readUUID() : null;
        boolean hasState = buf.readBoolean();
        BlockState state = null;
        if (hasState) {
            state = Block.stateById(buf.readVarInt());
        }
        boolean hasSecondState = buf.readBoolean();
        BlockState secondState = null;
        if (hasSecondState) {
            secondState = Block.stateById(buf.readVarInt());
        }
        int count = buf.readVarInt();
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            contents.add(buf.readItem());
        }
        boolean hasPos = buf.readBoolean();
        BlockPos pos = hasPos ? buf.readBlockPos() : null;
        boolean hasSecondPos = buf.readBoolean();
        BlockPos secondPos = hasSecondPos ? buf.readBlockPos() : null;
        boolean hasNbt = buf.readBoolean();
        CompoundTag nbt = hasNbt ? buf.readNbt() : null;
        boolean hasSecondNbt = buf.readBoolean();
        CompoundTag secondNbt = hasSecondNbt ? buf.readNbt() : null;
        return new CarryData(type, entityId, uuid, state, secondState, contents, pos, secondPos, nbt, secondNbt);
    }

    public CarryType carryType() { return carryType; }
    public int entityId() { return entityId; }
    public UUID entityUUID() { return entityUUID; }
    public BlockState blockState() { return blockState; }
    public BlockState secondBlockState() { return secondBlockState; }
    public List<ItemStack> blockContents() { return blockContents; }
    public BlockPos blockPos() { return blockPos; }
    public BlockPos secondBlockPos() { return secondBlockPos; }
    public CompoundTag blockNbt() { return blockNbt; }
    public CompoundTag secondBlockNbt() { return secondBlockNbt; }
    public float throwPower() { return throwPower; }
    public boolean aiming() { return aiming; }
    public float oscillationDirection() { return oscillationDirection; }

    public boolean isEmpty() { return carryType == CarryType.NONE; }

    public int litTime() { return litTime; }
    public int cookTime() { return cookTime; }
    public int cookTimeTotal() { return cookTimeTotal; }

    public void setLitTime(int litTime) { this.litTime = litTime; }
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }
    public void setCookTimeTotal(int cookTimeTotal) { this.cookTimeTotal = cookTimeTotal; }
    public void setBlockContents(List<ItemStack> blockContents) {
        this.blockContents = blockContents != null ? List.copyOf(blockContents) : List.of();
    }

    public CarryData withThrowPower(float power) {
        this.throwPower = power;
        return this;
    }

    public CarryData withAiming(boolean aiming) {
        this.aiming = aiming;
        return this;
    }

    public CarryData withOscillationDirection(float dir) {
        this.oscillationDirection = dir;
        return this;
    }
}