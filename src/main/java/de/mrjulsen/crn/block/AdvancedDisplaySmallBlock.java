package de.mrjulsen.crn.block;

import java.util.Map;
import de.mrjulsen.crn.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplaySmallBlock extends AbstractAdvancedDisplayBlock {
    
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    private static final Map<Direction, Map<Half, VoxelShape>> SHAPES = Map.of(
        Direction.SOUTH, Map.of(
            Half.BOTTOM, Block.box(0, 0, 0, 16, 8, 8),
            Half.TOP, Block.box(0, 8, 0, 16, 16, 8)
        ),
        Direction.WEST, Map.of(
            Half.BOTTOM, Block.box(8, 0, 0, 16, 8, 16),
            Half.TOP, Block.box(8, 8, 0, 16, 16, 16)
        ),
        Direction.NORTH, Map.of(
            Half.BOTTOM, Block.box(0, 0, 8, 16, 8, 16),
            Half.TOP, Block.box(0, 8, 8, 16, 16, 16)
        ),
        Direction.EAST, Map.of(
            Half.BOTTOM, Block.box(0, 0, 0, 8, 8, 16),
            Half.TOP, Block.box(0, 8, 0, 8, 16, 16)
        )
    );

    public AdvancedDisplaySmallBlock(Properties properties) {
        super(properties);
		registerDefaultState(defaultBlockState().setValue(HALF, Half.BOTTOM));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING)).get(pState.getValue(HALF));
    }

    @Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		BlockState stateForPlacement = super.getStateForPlacement(pContext);
		Direction direction = pContext.getClickedFace();
		if (direction == Direction.UP)
			return stateForPlacement;
		if (direction == Direction.DOWN || (pContext.getClickLocation().y - pContext.getClickedPos().getY() > 0.5D))
			return stateForPlacement.setValue(HALF, Half.TOP);
		return stateForPlacement;
	}

    @Override
    public boolean canConnectWithBlock(Level level, BlockPos selfPos, BlockPos otherPos) {
        return level.getBlockState(otherPos).getBlock() instanceof AdvancedDisplaySmallBlock && level.getBlockState(selfPos).getValue(HALF) == level.getBlockState(otherPos).getValue(HALF);
    }

    @Override
    protected boolean canConnect(LevelAccessor level, BlockPos pos, BlockState state, BlockState other) {
        return super.canConnect(level, pos, state, other) && state.getValue(HALF) == other.getValue(HALF);
    }

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(HALF));
	}

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 0.5F);
    }

    @Override
    public Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(0.0f, blockState.getValue(HALF) == Half.BOTTOM ? 8.0F : 0.0F);
    }

    @Override
    public Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(8.05f, 16.05f);
    }
}
