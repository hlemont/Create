package com.simibubi.create.modules.contraptions.relays.elementary;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.AllShapes;
import com.simibubi.create.modules.contraptions.base.IRotate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class CogWheelBlock extends ShaftBlock {

	private boolean isLarge;

	public CogWheelBlock(boolean large) {
		super(Properties.from(Blocks.GRANITE));
		isLarge = large;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return (isLarge ? AllShapes.LARGE_GEAR : AllShapes.SMALL_GEAR).get(state.get(AXIS));
	}

	@Override
	public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
		for (Direction facing : Direction.values()) {
			if (facing.getAxis() == state.get(AXIS))
				continue;

			BlockState blockState = worldIn.getBlockState(pos.offset(facing));
			if (AllBlocks.LARGE_COGWHEEL.typeOf(blockState) || isLarge && AllBlocks.COGWHEEL.typeOf(blockState))
				return false;
		}
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		BlockPos placedOnPos = context.getPos().offset(context.getFace().getOpposite());
		BlockState placedAgainst = context.getWorld().getBlockState(placedOnPos);
		Block block = placedAgainst.getBlock();

		if (!(block instanceof IRotate) || !(((IRotate) block).hasCogsTowards(context.getWorld(), placedOnPos,
				placedAgainst, context.getFace())))
			return super.getStateForPlacement(context);

		return getDefaultState().with(AXIS, ((IRotate) block).getRotationAxis(placedAgainst));
	}

	@Override
	public float getParticleTargetRadius() {
		return isLarge ? 1.125f : .65f;
	}

	@Override
	public float getParticleInitialRadius() {
		return isLarge ? 1f : .75f;
	}

	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
		items.add(new ItemStack(this));
	}

	// IRotate

	@Override
	public boolean hasCogsTowards(World world, BlockPos pos, BlockState state, Direction face) {
		return !isLarge && face.getAxis() != state.get(AXIS);
	}

}
