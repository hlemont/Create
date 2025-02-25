package com.simibubi.create.compat.tconstruct;

import com.simibubi.create.api.behaviour.BlockSpoutingBehaviour;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.fluids.actors.SpoutTileEntity;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.fluid.FluidHelper;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.IFluidHandler;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpoutCasting extends BlockSpoutingBehaviour {

	static Boolean TICON_PRESENT = null;

	ResourceLocation TABLE = new ResourceLocation("tconstruct", "table");
	ResourceLocation BASIN = new ResourceLocation("tconstruct", "basin");

	@Override
	public long fillBlock(Level level, BlockPos pos, SpoutTileEntity spout, FluidStack availableFluid,
		boolean simulate) {
		if (!enabled())
			return 0;

		BlockEntity te = level.getBlockEntity(pos);
		if (te == null)
			return 0;

		IFluidHandler handler = TransferUtil.getFluidHandler(te, Direction.UP)
			.orElse(null);
		if (handler == null)
			return 0;
		if (handler.getTanks() != 1)
			return 0;

		ResourceLocation registryName = Registry.BLOCK_ENTITY_TYPE.getKey(te.getType());
		if (!registryName.equals(TABLE) && !registryName.equals(BASIN))
			return 0;
		if (!handler.isFluidValid(0, availableFluid))
			return 0;

		FluidStack containedFluid = handler.getFluidInTank(0);
		if (!(containedFluid.isEmpty() || containedFluid.isFluidEqual(availableFluid)))
			return 0;

		// Do not fill if it would only partially fill the table (unless > 1000mb)
		long amount = availableFluid.getAmount();
		if (amount < FluidConstants.BUCKET
			&& handler.fill(FluidHelper.copyStackWithAmount(availableFluid, amount + 1), true) > amount)
			return 0;

		// Return amount filled into the table/basin
		return handler.fill(availableFluid, simulate ? true : false);
	}

	private boolean enabled() {
		if (TICON_PRESENT == null)
			TICON_PRESENT = Mods.TCONSTRUCT.isLoaded();
		if (!TICON_PRESENT)
			return false;
		return AllConfigs.SERVER.recipes.allowCastingBySpout.get();
	}

}
