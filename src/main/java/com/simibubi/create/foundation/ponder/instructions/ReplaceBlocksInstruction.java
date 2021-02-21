package com.simibubi.create.foundation.ponder.instructions;

import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.Selection;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class ReplaceBlocksInstruction extends WorldModifyInstruction {

	private BlockState stateToUse;
	private boolean replaceAir;
	private boolean spawnParticles;

	public ReplaceBlocksInstruction(Selection selection, BlockState stateToUse, boolean replaceAir,
		boolean spawnParticles) {
		super(selection);
		this.stateToUse = stateToUse;
		this.replaceAir = replaceAir;
		this.spawnParticles = spawnParticles;
	}

	@Override
	protected void runModification(Selection selection, PonderScene scene) {
		PonderWorld world = scene.getWorld();
		selection.forEach(pos -> {
			if (!world.getBounds()
				.isVecInside(pos))
				return;
			BlockState prevState = world.getBlockState(pos);
			if (!replaceAir && prevState == Blocks.AIR.getDefaultState())
				return;
			if (spawnParticles)
				world.addBlockDestroyEffects(pos, prevState);
			world.setBlockState(pos, stateToUse);
		});
	}

	@Override
	protected boolean needsRedraw() {
		return true;
	}

}
