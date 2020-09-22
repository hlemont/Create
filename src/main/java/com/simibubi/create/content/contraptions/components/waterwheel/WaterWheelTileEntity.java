package com.simibubi.create.content.contraptions.components.waterwheel;

import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;

public class WaterWheelTileEntity extends GeneratingKineticTileEntity {

	private Map<Direction, Float> flows;

	public WaterWheelTileEntity(TileEntityType<? extends WaterWheelTileEntity> type) {
		super(type);
		flows = new HashMap<>();
		for (Direction d : Direction.values())
			setFlow(d, 0);
		setLazyTickRate(20);
	}

	@Override
	protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
		super.fromTag(state, compound, clientPacket);
		if (compound.contains("Flows")) {
			for (Direction d : Direction.values())
				setFlow(d, compound.getCompound("Flows")
					.getFloat(d.getString()));
		}
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos).grow(1);
	}

	@Override
	public void write(CompoundNBT compound, boolean clientPacket) {
		CompoundNBT flows = new CompoundNBT();
		for (Direction d : Direction.values())
			flows.putFloat(d.getString(), this.flows.get(d));
		compound.put("Flows", flows);

		super.write(compound, clientPacket);
	}

	public void setFlow(Direction direction, float speed) {
		flows.put(direction, speed);
		markDirty();
	}

	@Override
	public float getGeneratedSpeed() {
		float speed = 0;
		for (Float f : flows.values())
			speed += f;
		return speed;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		AllBlocks.WATER_WHEEL.get()
			.updateAllSides(getBlockState(), world, pos);
	}

}
