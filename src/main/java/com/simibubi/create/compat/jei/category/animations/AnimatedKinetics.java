package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocksNew;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction.Axis;

public abstract class AnimatedKinetics implements IDrawable {

	public static float getCurrentAngle() {
		return ((AnimationTickHolder.ticks + Minecraft.getInstance().getRenderPartialTicks()) * 4f) % 360;
	}
	
	protected BlockState shaft(Axis axis) {
		return AllBlocksNew.getDefault(AllBlocksNew.SHAFT).with(BlockStateProperties.AXIS, axis);
	}
	
	protected AllBlockPartials cogwheel() {
		return AllBlockPartials.SHAFTLESS_COGWHEEL;
	}
	
	@Override
	public int getWidth() {
		return 50;
	}

	@Override
	public int getHeight() {
		return 50;
	}

}
