package net.neoforge.grabthat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.neoforge.grabthat.data.CarryData;
import net.neoforge.grabthat.data.CarryType;
import net.neoforge.grabthat.util.CarryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

public class CarryRenderer {

    private static final float CARRIED_SCALE = 0.6f;
    private static final float BED_Y_OFFSET = -0.7f;

    private static ModelPart singleChestModel;
    private static ModelPart doubleChestLeftModel;
    private static ModelPart doubleChestRightModel;

    private CarryRenderer() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(CarryRenderer.class);
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        net.minecraft.world.entity.Entity entity = event.getEntity();
        if (!isCarriedMob(entity)) return;

        if (entity instanceof LivingEntity living) {

            living.walkAnimation.update(0.0F, 1.0F);
            living.walkAnimation.update(0.0F, 1.0F);

            PoseStack poseStack = event.getPoseStack();
            float partialTick = event.getPartialTick();
            float yaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);

            if (!(living instanceof net.minecraft.world.entity.animal.Chicken)) {
                boolean upright = net.neoforge.grabthat.util.CarryUtil.isUprightMob(living);

                if (upright) {

                    poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
                } else {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
                }
            }
        }
    }

    private static boolean isCarriedMob(net.minecraft.world.entity.Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || entity.isRemoved()) return false;
        if (entity instanceof Player) return false;
        for (Player carrier : mc.level.players()) {
            CarryData d = CarryClientContext.get(carrier.getUUID());
            if (d.carryType() == CarryType.MOB && d.entityId() == entity.getId()) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double camX = event.getCamera().getPosition().x;
        double camY = event.getCamera().getPosition().y;
        double camZ = event.getCamera().getPosition().z;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        for (Player player : mc.level.players()) {
            CarryData data = CarryClientContext.get(player.getUUID());
            if (data.isEmpty()) continue;

            if (data.carryType() == CarryType.BLOCK) {
                renderCarriedBlock(player, data, event.getPoseStack(), partialTick, camX, camY, camZ);
            }
        }
    }

    private static int getPackedLight(Player player, BlockPos pos) {
        int block = player.level().getBrightness(LightLayer.BLOCK, pos);
        int sky = player.level().getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(block, sky);
    }

    private static void renderCarriedBlock(Player player, CarryData data, PoseStack poseStack,
                                           float partialTick, double camX, double camY, double camZ) {
        Minecraft mc = Minecraft.getInstance();
        BlockState state = data.blockState();
        if (state == null) return;

        if (state.getBlock() instanceof AbstractChestBlock) {
            renderCarriedChest(player, data, poseStack, partialTick, camX, camY, camZ);
            return;
        }

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        Vec3 pos = player.getPosition(partialTick);
        double bx = pos.x;
        double by = pos.y + 2.05;
        double bz = pos.z;

        int packedLight = getPackedLight(player, BlockPos.containing(bx, by, bz));

        poseStack.pushPose();
        poseStack.translate(bx - camX, by - camY, bz - camZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw(player, state) - player.getYRot()));
        poseStack.scale(CARRIED_SCALE, CARRIED_SCALE, CARRIED_SCALE);

        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART)) {
            renderCarriedBed(player, data, state, poseStack, buffer, packedLight);
        } else {
            poseStack.translate(-0.5, -0.5, -0.5);
            BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
            blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY,
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                    null);

            BlockState secondState = data.secondBlockState();
            if (secondState != null) {
                BlockPos offset = CarryUtil.secondPartOffset(state, data.blockPos(), data.secondBlockPos());
                if (offset != null) {
                    poseStack.pushPose();
                    poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
                    blockRenderer.renderSingleBlock(secondState, poseStack, buffer, packedLight,
                            OverlayTexture.NO_OVERLAY,
                            net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                            null);
                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
        buffer.endBatch();
    }

    private static void renderCarriedBed(Player player, CarryData data, BlockState state,
                                         PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                         int packedLight) {
        BlockState secondState = data.secondBlockState();
        BlockPos offset = CarryUtil.secondPartOffset(state, data.blockPos(), data.secondBlockPos());
        if (secondState == null || offset == null) return;

        poseStack.pushPose();
        poseStack.translate(-(offset.getX() * 0.5 + 0.5), BED_Y_OFFSET, -(offset.getZ() * 0.5 + 0.5));

        renderBedPart(player, data.blockNbt(), state, poseStack, buffer, packedLight);

        poseStack.pushPose();
        poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
        renderBedPart(player, data.secondBlockNbt(), secondState, poseStack, buffer, packedLight);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderBedPart(Player player, CompoundTag nbt, BlockState state,
                                      PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                      int packedLight) {
        BlockEntity be = BlockEntityType.BED.create(BlockPos.ZERO, state);
        if (be == null) return;

        be.setLevel(player.level());
        if (nbt != null) {
            be.loadWithComponents(nbt, player.level().registryAccess());
        }

        BlockEntityRenderer<BlockEntity> renderer =
                Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer == null) return;

        renderer.render(be, 0.0F, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static float facingYaw(Player player, BlockState state) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
        if (facing == null) return 0.0f;
        return switch (facing) {
            case EAST -> -90f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            default -> 180f;
        };
    }

    private static void renderCarriedChest(Player player, CarryData data, PoseStack poseStack,
                                           float partialTick, double camX, double camY, double camZ) {
        Minecraft mc = Minecraft.getInstance();
        BlockState state = data.blockState();
        if (state == null) return;

        Vec3 pos = player.getPosition(partialTick);
        double bx = pos.x;
        double by = pos.y + 2.2;
        double bz = pos.z;

        BlockState secondState = data.secondBlockState();
        boolean doubleChest = secondState != null
                && state.getBlock() instanceof AbstractChestBlock
                && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;

        float yaw = -player.getYRot();
        if (doubleChest) {
            yaw += 90.0f;
        }

        ensureChestModels();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        int packedLight = getPackedLight(player, BlockPos.containing(bx, by, bz));

        poseStack.pushPose();
        poseStack.translate(bx - camX, by - camY, bz - camZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(CARRIED_SCALE, CARRIED_SCALE, CARRIED_SCALE);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        if (!doubleChest) {
            ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
            Material material = chestMaterial(state, type, false);
            renderChestPart(singleChestModel, material.buffer(buffer, RenderType::entityCutout),
                    poseStack, packedLight);
} else {

            poseStack.translate(-0.5f, 0.0f, 0.0f);
            renderChestPart(doubleChestRightModel,
                    chestMaterial(state, ChestType.RIGHT, true).buffer(buffer, RenderType::entityCutout),
                    poseStack, packedLight);
            poseStack.translate(1.0f, 0.0f, 0.0f);
            renderChestPart(doubleChestLeftModel,
                    chestMaterial(secondState, ChestType.LEFT, true).buffer(buffer, RenderType::entityCutout),
                    poseStack, packedLight);
        }

        poseStack.popPose();
        buffer.endBatch();
    }

    private static void renderChestPart(ModelPart model, VertexConsumer consumer, PoseStack poseStack, int packedLight) {
        model.getChild("lid").render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        model.getChild("lock").render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        model.getChild("bottom").render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static Material chestMaterial(BlockState state, ChestType type, boolean doubleChest) {
        if (state.getBlock() instanceof EnderChestBlock) {
            return Sheets.ENDER_CHEST_LOCATION;
        }
        boolean trapped = state.getBlock() instanceof TrappedChestBlock;
        if (!doubleChest || type == ChestType.SINGLE) {
            return trapped ? Sheets.CHEST_TRAP_LOCATION : Sheets.CHEST_LOCATION;
        }
        return switch (type) {
            case LEFT -> trapped ? Sheets.CHEST_TRAP_LOCATION_LEFT : Sheets.CHEST_LOCATION_LEFT;
            case RIGHT -> trapped ? Sheets.CHEST_TRAP_LOCATION_RIGHT : Sheets.CHEST_LOCATION_RIGHT;
            default -> trapped ? Sheets.CHEST_TRAP_LOCATION : Sheets.CHEST_LOCATION;
        };
    }

    private static void ensureChestModels() {
        if (singleChestModel != null) return;
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        singleChestModel = models.bakeLayer(ModelLayers.CHEST);
        doubleChestLeftModel = models.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        doubleChestRightModel = models.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
    }
}