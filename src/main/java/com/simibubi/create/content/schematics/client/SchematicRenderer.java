package com.simibubi.create.content.schematics.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.render.TileEntityRenderHelper;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;

public class SchematicRenderer {

	private final Map<RenderType, SuperByteBuffer> bufferCache = new HashMap<>(getLayerCount());
	private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());
	private final Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
	private boolean active;
	private boolean changed;
	protected SchematicWorld schematic;
	private BlockPos anchor;

	public SchematicRenderer() {
		changed = false;
	}

	public void display(SchematicWorld world) {
		this.anchor = world.anchor;
		this.schematic = world;
		this.active = true;
		this.changed = true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void update() {
		changed = true;
	}

	public void tick() {
		if (!active)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !changed)
			return;

		redraw(mc);
		changed = false;
	}

	public void render(PoseStack ms, SuperRenderTypeBuffer buffer) {
		if (!active)
			return;
		buffer.getBuffer(RenderType.solid());
		for (RenderType layer : RenderType.chunkBufferLayers()) {
			if (!usedBlockRenderLayers.contains(layer))
				continue;
			SuperByteBuffer superByteBuffer = bufferCache.get(layer);
			superByteBuffer.renderInto(ms, buffer.getBuffer(layer));
		}
		TileEntityRenderHelper.renderTileEntities(schematic, schematic.getRenderedTileEntities(), ms, buffer);
	}

	protected void redraw(Minecraft minecraft) {
		usedBlockRenderLayers.clear();
		startedBufferBuilders.clear();

		final SchematicWorld blockAccess = schematic;
		final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();

		List<BlockState> blockstates = new LinkedList<>();
		Map<RenderType, BufferBuilder> buffers = new HashMap<>();
		PoseStack ms = new PoseStack();

		BlockPos.betweenClosedStream(blockAccess.getBounds())
			.forEach(localPos -> {
				ms.pushPose();
				MatrixTransformStack.of(ms)
					.translate(localPos);
				BlockPos pos = localPos.offset(anchor);
				BlockState state = blockAccess.getBlockState(pos);

				for (RenderType blockRenderLayer : RenderType.chunkBufferLayers()) {
					if (!ItemBlockRenderTypes.canRenderInLayer(state, blockRenderLayer))
						continue;
					ForgeHooksClient.setRenderLayer(blockRenderLayer);
					if (!buffers.containsKey(blockRenderLayer))
						buffers.put(blockRenderLayer, new BufferBuilder(DefaultVertexFormat.BLOCK.getIntegerSize()));

					BufferBuilder bufferBuilder = buffers.get(blockRenderLayer);
					if (startedBufferBuilders.add(blockRenderLayer))
						bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.BLOCK);

					BlockEntity tileEntity = blockAccess.getBlockEntity(localPos);

					if (blockRendererDispatcher.renderModel(state, pos, blockAccess, ms, bufferBuilder, true,
						minecraft.level.random,
						tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
						usedBlockRenderLayers.add(blockRenderLayer);
					}
					blockstates.add(state);
				}

				ForgeHooksClient.setRenderLayer(null);
				ms.popPose();
			});

		// finishDrawing
		for (RenderType layer : RenderType.chunkBufferLayers()) {
			if (!startedBufferBuilders.contains(layer))
				continue;
			BufferBuilder buf = buffers.get(layer);
			buf.end();
			bufferCache.put(layer, new SuperByteBuffer(buf));
		}
	}

	private static int getLayerCount() {
		return RenderType.chunkBufferLayers()
			.size();
	}

}
