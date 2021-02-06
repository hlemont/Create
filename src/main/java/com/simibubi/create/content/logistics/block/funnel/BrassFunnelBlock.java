package com.simibubi.create.content.logistics.block.funnel;

import com.simibubi.create.AllBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class BrassFunnelBlock extends FunnelBlock {

	public BrassFunnelBlock(Properties p_i48415_1_) {
		super(p_i48415_1_);
	}

	@Override
	public BlockState getEquivalentBeltFunnel(IBlockReader world, BlockPos pos, BlockState state) {
		Direction facing = state.get(HORIZONTAL_FACING);
		return AllBlocks.BRASS_BELT_FUNNEL.getDefaultState()
			.with(BeltFunnelBlock.HORIZONTAL_FACING, facing)
			.with(POWERED, state.get(POWERED));
	}

}
