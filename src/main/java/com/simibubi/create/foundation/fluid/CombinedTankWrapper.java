package com.simibubi.create.foundation.fluid;

import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.IFluidHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.EmptyHandler;

/**
 * Combines multiple IFluidHandlers into one interface (See CombinedInvWrapper
 * for items)
 */
public class CombinedTankWrapper implements IFluidHandler {

	protected final IFluidHandler[] itemHandler;
	protected final int[] baseIndex;
	protected final int tankCount;
	protected boolean enforceVariety;

	public CombinedTankWrapper(IFluidHandler... fluidHandlers) {
		this.itemHandler = fluidHandlers;
		this.baseIndex = new int[fluidHandlers.length];
		int index = 0;
		for (int i = 0; i < fluidHandlers.length; i++) {
			index += fluidHandlers[i].getTanks();
			baseIndex[i] = index;
		}
		this.tankCount = index;
	}

	public CombinedTankWrapper enforceVariety() {
		enforceVariety = true;
		return this;
	}

	@Override
	public int getTanks() {
		return tankCount;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		int index = getIndexForSlot(tank);
		IFluidHandler handler = getHandlerFromIndex(index);
		tank = getSlotFromIndex(tank, index);
		return handler.getFluidInTank(tank);
	}

	@Override
	public long getTankCapacity(int tank) {
		int index = getIndexForSlot(tank);
		IFluidHandler handler = getHandlerFromIndex(index);
		int localSlot = getSlotFromIndex(tank, index);
		return handler.getTankCapacity(localSlot);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		int index = getIndexForSlot(tank);
		IFluidHandler handler = getHandlerFromIndex(index);
		int localSlot = getSlotFromIndex(tank, index);
		return handler.isFluidValid(localSlot, stack);
	}

	@Override
	public long fill(FluidStack resource, boolean sim) {
		if (resource.isEmpty())
			return 0;

		int filled = 0;
		resource = resource.copy();

		boolean fittingHandlerFound = false;
		Outer: for (boolean searchPass : Iterate.trueAndFalse) {
			for (IFluidHandler iFluidHandler : itemHandler) {

				for (int i = 0; i < iFluidHandler.getTanks(); i++)
					if (searchPass && iFluidHandler.getFluidInTank(i)
						.isFluidEqual(resource))
						fittingHandlerFound = true;

				if (searchPass && !fittingHandlerFound)
					continue;

				long filledIntoCurrent = iFluidHandler.fill(resource, sim);
				resource.shrink(filledIntoCurrent);
				filled += filledIntoCurrent;

				if (resource.isEmpty() || fittingHandlerFound || enforceVariety && filledIntoCurrent != 0)
					break Outer;
			}
		}

		return filled;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean sim) {
		if (resource.isEmpty())
			return resource;

		FluidStack drained = FluidStack.EMPTY;
		resource = resource.copy();

		for (IFluidHandler iFluidHandler : itemHandler) {
			FluidStack drainedFromCurrent = iFluidHandler.drain(resource, sim);
			long amount = drainedFromCurrent.getAmount();
			resource.shrink(amount);

			if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || drainedFromCurrent.isFluidEqual(drained)))
				drained = new FluidStack(drainedFromCurrent.getType(), amount + drained.getAmount(),
					drainedFromCurrent.getTag());
			if (resource.isEmpty())
				break;
		}

		return drained;
	}

	@Override
	public FluidStack drain(long maxDrain, boolean sim) {
		FluidStack drained = FluidStack.EMPTY;

		for (IFluidHandler iFluidHandler : itemHandler) {
			FluidStack drainedFromCurrent = iFluidHandler.drain(maxDrain, sim);
			long amount = drainedFromCurrent.getAmount();
			maxDrain -= amount;

			if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || drainedFromCurrent.isFluidEqual(drained)))
				drained = new FluidStack(drainedFromCurrent.getType(), amount + drained.getAmount(),
					drainedFromCurrent.getTag());
			if (maxDrain == 0)
				break;
		}

		return drained;
	}

	protected int getIndexForSlot(int slot) {
		if (slot < 0)
			return -1;
		for (int i = 0; i < baseIndex.length; i++)
			if (slot - baseIndex[i] < 0)
				return i;
		return -1;
	}

	protected IFluidHandler getHandlerFromIndex(int index) {
		if (index < 0 || index >= itemHandler.length)
			return (IFluidHandler) EmptyHandler.INSTANCE;
		return itemHandler[index];
	}

	protected int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndex.length)
			return slot;
		return slot - baseIndex[index - 1];
	}
}
