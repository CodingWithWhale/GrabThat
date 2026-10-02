package net.neoforge.grabthat.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CarryData {
    public static final CarryData EMPTY = new CarryData(CarryType.NONE, -1, null, null, List.of(), null);

    public static final Codec<CarryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("carry_type_ordinal").forGetter(d -> d.carryType.ordinal()),
            Codec.INT.fieldOf("entity_id").forGetter(d -> d.entityId),
            Codec.STRING.optionalFieldOf("entity_uuid").forGetter(d -> Optional.ofNullable(d.entityUUID != null ? d.entityUUID.toString() : null)),
            Codec.INT.optionalFieldOf("block_state_id").forGetter(d -> d.blockState() != null ? Optional.of(Block.getId(d.blockState())) : Optional.empty()),
            Codec.INT.optionalFieldOf("second_block_state_id").forGetter(d -> d.secondBlockState != null ? Optional.of(Block.getId(d.secondBlockState)) : Optional.empty()),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("block_contents", List.of()).forGetter(d -> d.blockContents),
            BlockPos.CODEC.optionalFieldOf("block_pos").forGetter(d -> Optional.ofNullable(d.blockPos)),
            BlockPos.CODEC.optionalFieldOf("second_block_pos").forGetter(d -> Optional.ofNullable(d.secondBlockPos)),
            CompoundTag.CODEC.optionalFieldOf("block_nbt").forGetter(d -> Optional.ofNullable(d.blockNbt)),
            CompoundTag.CODEC.optionalFieldOf("second_block_nbt").forGetter(d -> Optional.ofNullable(d.secondBlockNbt))
    ).apply(instance, (ordinal, entityId, uuid, stateId, secondStateId, contents, pos, secondPos, nbt, secondNbt) ->
            new CarryData(CarryType.VALUES[ordinal % CarryType.VALUES.length],
                    entityId, uuid.map(UUID::fromString).orElse(null),
                    stateId.map(Block::stateById).orElse(null),
                    secondStateId.map(Block::stateById).orElse(null),
                    contents, pos.orElse(null), secondPos.orElse(null), nbt.orElse(null), secondNbt.orElse(null))
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarryData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CarryData decode(RegistryFriendlyByteBuf buf) {
            CarryType type = CarryType.STREAM_CODEC.decode(buf);
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
                contents.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
            boolean hasPos = buf.readBoolean();
            BlockPos pos = hasPos ? BlockPos.STREAM_CODEC.decode(buf) : null;
            boolean hasSecondPos = buf.readBoolean();
            BlockPos secondPos = hasSecondPos ? BlockPos.STREAM_CODEC.decode(buf) : null;
            boolean hasNbt = buf.readBoolean();
            CompoundTag nbt = hasNbt ? buf.readNbt() : null;
            boolean hasSecondNbt = buf.readBoolean();
            CompoundTag secondNbt = hasSecondNbt ? buf.readNbt() : null;
            return new CarryData(type, entityId, uuid, state, secondState, contents, pos, secondPos, nbt, secondNbt);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CarryData data) {
            CarryType.STREAM_CODEC.encode(buf, data.carryType);
            buf.writeInt(data.entityId);
            buf.writeBoolean(data.entityUUID != null);
            if (data.entityUUID != null) buf.writeUUID(data.entityUUID);
            buf.writeBoolean(data.blockState != null);
            if (data.blockState != null) buf.writeVarInt(Block.getId(data.blockState));
            buf.writeBoolean(data.secondBlockState != null);
            if (data.secondBlockState != null) buf.writeVarInt(Block.getId(data.secondBlockState));
            buf.writeVarInt(data.blockContents.size());
            for (ItemStack stack : data.blockContents) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            }
            buf.writeBoolean(data.blockPos != null);
            if (data.blockPos != null) BlockPos.STREAM_CODEC.encode(buf, data.blockPos);
            buf.writeBoolean(data.secondBlockPos != null);
            if (data.secondBlockPos != null) BlockPos.STREAM_CODEC.encode(buf, data.secondBlockPos);
            buf.writeBoolean(data.blockNbt != null);
            if (data.blockNbt != null) buf.writeNbt(data.blockNbt);
            buf.writeBoolean(data.secondBlockNbt != null);
            if (data.secondBlockNbt != null) buf.writeNbt(data.secondBlockNbt);
        }
    };

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
