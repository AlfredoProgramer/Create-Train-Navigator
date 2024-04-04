package de.mrjulsen.crn.client.ber.variants;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.client.ber.base.AbstractBlockEntityRenderInstance;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IBERRenderSubtype<T extends BlockEntity, S extends AbstractBlockEntityRenderInstance<T>, U> {
    void update(Level level, BlockPos pos, BlockState state, T blockEntity, S parent, EUpdateReason reason);
    boolean isSingleLined();
    default void renderAdditional(BlockEntityRendererContext context, T pBlockEntity, S parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, U renderData) {}
    default void tick(Level level, BlockPos pos, BlockState state, T pBlockEntity, S parent) {}
}
