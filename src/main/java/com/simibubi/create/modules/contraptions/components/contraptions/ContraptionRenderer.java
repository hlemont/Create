package com.simibubi.create.modules.contraptions.components.contraptions;

import java.util.Random;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.PlacementSimulationWorld;
import com.simibubi.create.foundation.utility.SuperByteBuffer;
import com.simibubi.create.foundation.utility.SuperByteBufferCache.Compartment;
import com.simibubi.create.foundation.utility.render.StructureRenderer;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.client.model.data.EmptyModelData;

public class ContraptionRenderer {

	public static final Compartment<Contraption> CONTRAPTION = new Compartment<>();
	protected static PlacementSimulationWorld renderWorld;

	public static void render(World world, Contraption c, Consumer<SuperByteBuffer> transform, BufferBuilder buffer) {
		SuperByteBuffer contraptionBuffer = CreateClient.bufferCache.get(CONTRAPTION, c, () -> renderContraption(c));
		transform.accept(contraptionBuffer);
		contraptionBuffer.light((lx, ly, lz) -> getLight(world, lx, ly, lz)).renderInto(buffer);
		renderActors(world, c, transform, buffer);
	}

	public static void renderTEsWithGL(World world, Contraption c, Vec3d position, Vec3d rotation) {
		StructureRenderer.renderTileEntities(world, position, rotation, c.customRenderTEs);
	}

	private static SuperByteBuffer renderContraption(Contraption c) {
		if (renderWorld == null || renderWorld.getWorld() != Minecraft.getInstance().world)
			renderWorld = new PlacementSimulationWorld(Minecraft.getInstance().world);

		BlockRendererDispatcher dispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockModelRenderer blockRenderer = dispatcher.getBlockModelRenderer();
		Random random = new Random();
		BufferBuilder builder = new BufferBuilder(0);
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		builder.setTranslation(0, 0, 0);

		for (BlockInfo info : c.blocks.values())
			renderWorld.setBlockState(info.pos, info.state);
		for (BlockPos pos : c.renderOrder) {
			BlockInfo info = c.blocks.get(pos);
			BlockState state = info.state;

			if (state.getRenderType() == BlockRenderType.ENTITYBLOCK_ANIMATED)
				continue;

			IBakedModel originalModel = dispatcher.getModelForState(state);
			blockRenderer.renderModel(renderWorld, originalModel, state, info.pos, builder, true, random, 42,
					EmptyModelData.INSTANCE);
		}

		builder.finishDrawing();
		renderWorld.clear();
		return new SuperByteBuffer(builder.getByteBuffer());
	}

	private static void renderActors(World world, Contraption c, Consumer<SuperByteBuffer> transform,
			BufferBuilder buffer) {
		for (Pair<BlockInfo, MovementContext> actor : c.getActors()) {
			MovementContext context = actor.getRight();
			if (context == null)
				continue;
			if (context.world == null)
				context.world = world;

			BlockInfo blockInfo = actor.getLeft();
			for (SuperByteBuffer render : Contraption.getMovement(blockInfo.state).renderListInContraption(context)) {
				if (render == null)
					continue;

				int posX = blockInfo.pos.getX();
				int posY = blockInfo.pos.getY();
				int posZ = blockInfo.pos.getZ();

				render.translate(posX, posY, posZ);
				transform.accept(render);
				render.light((lx, ly, lz) -> getLight(world, lx, ly, lz)).renderInto(buffer);
			}
		}
	}

	public static int getLight(World world, float lx, float ly, float lz) {
		MutableBlockPos pos = new MutableBlockPos();
		float sky = 0, block = 0;
		float offset = 1 / 8f;

		for (float zOffset = offset; zOffset >= -offset; zOffset -= 2 * offset)
			for (float yOffset = offset; yOffset >= -offset; yOffset -= 2 * offset)
				for (float xOffset = offset; xOffset >= -offset; xOffset -= 2 * offset) {
					pos.setPos(lx + xOffset, ly + yOffset, lz + zOffset);
					sky += world.getLightFor(LightType.SKY, pos) / 8f;
					block += world.getLightFor(LightType.BLOCK, pos) / 8f;
				}

		return ((int) sky) << 20 | ((int) block) << 4;
	}

}
