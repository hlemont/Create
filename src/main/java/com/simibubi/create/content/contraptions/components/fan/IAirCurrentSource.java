package com.simibubi.create.content.contraptions.components.fan;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.CKinetics;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

@MethodsReturnNonnullByDefault
public interface IAirCurrentSource {
	@Nullable
	AirCurrent getAirCurrent();

	@Nullable
	Level getAirCurrentWorld();

	BlockPos getAirCurrentPos();

	float getSpeed();

	Direction getAirflowOriginSide();

	@Nullable
	Direction getAirFlowDirection();

	default float getMaxDistance() {
		float speed = Math.abs(this.getSpeed());
		CKinetics config = AllConfigs.SERVER.kinetics;
		float distanceFactor = Math.min(speed / config.fanRotationArgmax.get(), 1);
		float pushDistance = Mth.lerp(distanceFactor, 3, config.fanPushDistance.get());
		float pullDistance = Mth.lerp(distanceFactor, 3f, config.fanPullDistance.get());
		return this.getSpeed() > 0 ? pushDistance : pullDistance;
	}

	boolean isSourceRemoved();
}
