package com.simibubi.create.modules.contraptions.components.press;

import static net.minecraft.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.SuperByteBuffer;
import com.simibubi.create.modules.contraptions.base.KineticTileEntity;
import com.simibubi.create.modules.contraptions.base.KineticTileEntityRenderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;

public class MechanicalPressTileEntityRenderer extends KineticTileEntityRenderer {

	public MechanicalPressTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void renderSafe(KineticTileEntity te, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer,
			int light, int overlay) {
		super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

		BlockPos pos = te.getPos();
		BlockState blockState = te.getBlockState();
		int packedLightmapCoords =WorldRenderer.getLightmapCoordinates(te.getWorld(), blockState, pos);
		float renderedHeadOffset = ((MechanicalPressTileEntity) te).getRenderedHeadOffset(partialTicks);

		SuperByteBuffer headRender = AllBlockPartials.MECHANICAL_PRESS_HEAD.renderOnHorizontal(blockState);
		headRender.translate(0, -renderedHeadOffset, 0).light(packedLightmapCoords).renderInto(ms, buffer.getBuffer(RenderType.getSolid()));
	}

	@Override
	protected BlockState getRenderedBlockState(KineticTileEntity te) {
		return AllBlocks.SHAFT.get().getDefaultState().with(BlockStateProperties.AXIS,
				te.getBlockState().get(HORIZONTAL_FACING).getAxis());
	}

}
