package com.simibubi.create.content.logistics.block.redstone;

import java.util.Random;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.block.NeighborChangeListeningBlock;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StockpileSwitchBlock extends HorizontalDirectionalBlock implements ITE<StockpileSwitchTileEntity>, IWrenchable, NeighborChangeListeningBlock, ConnectableRedstoneBlock {

	public static final IntegerProperty INDICATOR = IntegerProperty.create("indicator", 0, 6);

	public StockpileSwitchBlock(Properties p_i48377_1_) {
		super(p_i48377_1_);
	}

	@Override
	public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		updateObservedInventory(state, worldIn, pos);
	}

	@Override
	public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
		if (world.isClientSide())
			return;
		if (!isObserving(state, pos, neighbor))
			return;
		updateObservedInventory(state, world, pos);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_,
		CollisionContext p_220053_4_) {
		return AllShapes.STOCKPILE_SWITCH.get(state.getValue(FACING));
	}

	private void updateObservedInventory(BlockState state, LevelReader world, BlockPos pos) {
		withTileEntityDo(world, pos, StockpileSwitchTileEntity::updateCurrentLevel);
	}

	private boolean isObserving(BlockState state, BlockPos pos, BlockPos observing) {
		return observing.equals(pos.relative(state.getValue(FACING)));
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side != null && side.getOpposite() != state.getValue(FACING);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
		if (side == blockState.getValue(FACING)
			.getOpposite())
			return 0;
		return getTileEntityOptional(blockAccess, pos).filter(StockpileSwitchTileEntity::isPowered)
			.map($ -> 15)
			.orElse(0);
	}

	@Override
	public void tick(BlockState blockState, ServerLevel world, BlockPos pos, Random random) {
		getTileEntityOptional(world, pos).ifPresent(StockpileSwitchTileEntity::updatePowerAfterDelay);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING, INDICATOR);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
		BlockHitResult hit) {
		if (player != null && AllItems.WRENCH.isIn(player.getItemInHand(handIn)))
			return InteractionResult.PASS;
		EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> withTileEntityDo(worldIn, pos, te -> this.displayScreen(te, player)));
		return InteractionResult.SUCCESS;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayScreen(StockpileSwitchTileEntity te, Player player) {
		if (player instanceof LocalPlayer)
			ScreenOpener.open(new StockpileSwitchScreen(te));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();

		Direction preferredFacing = null;
		for (Direction face : Iterate.horizontalDirections) {
			BlockEntity te = context.getLevel()
				.getBlockEntity(context.getClickedPos()
					.relative(face));
			if (te != null && (TransferUtil.getItemHandler(te)
				.isPresent()
				|| TransferUtil.getFluidHandler(te)
					.isPresent()))
				if (preferredFacing == null)
					preferredFacing = face;
				else {
					preferredFacing = null;
					break;
				}
		}

		if (preferredFacing != null) {
			state = state.setValue(FACING, preferredFacing);
		} else if (context.getClickedFace()
			.getAxis()
			.isHorizontal()) {
			state = state.setValue(FACING, context.getClickedFace());
		} else {
			state = state.setValue(FACING, context.getHorizontalDirection()
				.getOpposite());
		}

		return state;
	}

	@Override
	public Class<StockpileSwitchTileEntity> getTileEntityClass() {
		return StockpileSwitchTileEntity.class;
	}

	@Override
	public BlockEntityType<? extends StockpileSwitchTileEntity> getTileEntityType() {
		return AllTileEntities.STOCKPILE_SWITCH.get();
	}

}
