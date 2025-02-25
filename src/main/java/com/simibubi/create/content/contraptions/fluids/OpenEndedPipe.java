package com.simibubi.create.content.contraptions.fluids;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.contraptions.fluids.pipes.VanillaFluidTargets;
import com.simibubi.create.content.contraptions.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.IFluidHandler;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.LevelUtil;

import io.github.tropheusj.milk.Milk;
import me.alphamode.forgetags.Tags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

public class OpenEndedPipe extends FlowSource {

	private static final List<IEffectHandler> EFFECT_HANDLERS = new ArrayList<>();

	static {
		registerEffectHandler(new PotionEffectHandler());
		registerEffectHandler(new MilkEffectHandler());
		registerEffectHandler(new WaterEffectHandler());
		registerEffectHandler(new LavaEffectHandler());
	}

	private Level world;
	private BlockPos pos;
	private AABB aoe;

	private OpenEndFluidHandler fluidHandler;
	private BlockPos outputPos;
	private boolean wasPulling;

	private FluidStack cachedFluid;
	private List<MobEffectInstance> cachedEffects;

	public OpenEndedPipe(BlockFace face) {
		super(face);
		fluidHandler = new OpenEndFluidHandler();
		outputPos = face.getConnectedPos();
		pos = face.getPos();
		aoe = new AABB(outputPos).expandTowards(0, -1, 0);
		if (face.getFace() == Direction.DOWN)
			aoe = aoe.expandTowards(0, -1, 0);
	}

	public static void registerEffectHandler(IEffectHandler handler) {
		EFFECT_HANDLERS.add(handler);
	}

	public Level getWorld() {
		return world;
	}

	public BlockPos getPos() {
		return pos;
	}

	public BlockPos getOutputPos() {
		return outputPos;
	}

	public AABB getAOE() {
		return aoe;
	}

	@Override
	public void manageSource(Level world) {
		this.world = world;
	}

	@Override
	public LazyOptional<IFluidHandler> provideHandler() {
		return LazyOptional.of(() -> fluidHandler);
	}

	@Override
	public boolean isEndpoint() {
		return true;
	}

	public CompoundTag serializeNBT() {
		CompoundTag compound = new CompoundTag();
		fluidHandler.writeToNBT(compound);
		compound.putBoolean("Pulling", wasPulling);
		compound.put("Location", location.serializeNBT());
		return compound;
	}

	public static OpenEndedPipe fromNBT(CompoundTag compound, BlockPos tilePos) {
		BlockFace fromNBT = BlockFace.fromNBT(compound.getCompound("Location"));
		OpenEndedPipe oep = new OpenEndedPipe(new BlockFace(tilePos, fromNBT.getFace()));
		oep.fluidHandler.readFromNBT(compound);
		oep.wasPulling = compound.getBoolean("Pulling");
		return oep;
	}

	private FluidStack removeFluidFromSpace(boolean simulate) {
		FluidStack empty = FluidStack.EMPTY;
		if (world == null)
			return empty;
		if (!LevelUtil.isAreaLoaded(world, outputPos, 0))
			return empty;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.hasProperty(WATERLOGGED);

		FluidStack drainBlock = VanillaFluidTargets.drainBlock(world, outputPos, state, simulate);
		if (!drainBlock.isEmpty())
			return drainBlock;

		if (!waterlog && !state.getMaterial()
			.isReplaceable())
			return empty;
		if (fluidState.isEmpty() || !fluidState.isSource())
			return empty;

		FluidStack stack = new FluidStack(fluidState.getType(), FluidConstants.BUCKET);

		if (simulate)
			return stack;

		AllTriggers.triggerForNearbyPlayers(AllTriggers.PIPE_SPILL, world, pos, 5);

		if (waterlog) {
			world.setBlock(outputPos, state.setValue(WATERLOGGED, false), 3);
			world.scheduleTick(outputPos, Fluids.WATER, 1);
			return stack;
		}
		world.setBlock(outputPos, fluidState.createLegacyBlock()
			.setValue(LiquidBlock.LEVEL, 14), 3);
		return stack;
	}

	private boolean provideFluidToSpace(FluidStack fluid, boolean simulate) {
		if (world == null)
			return false;
		if (!LevelUtil.isAreaLoaded(world, outputPos, 0))
			return false;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.hasProperty(WATERLOGGED);

		if (!waterlog && !state.getMaterial()
			.isReplaceable())
			return false;
		if (fluid.isEmpty())
			return false;
		if (!FluidHelper.hasBlockState(fluid.getFluid()) || fluid.getFluid().is(Milk.MILK_TAG)) { // fabric: milk logic is different
			if (!simulate)
				applyEffects(fluid);
			return true;
		}

		if (!fluidState.isEmpty() && fluidState.getType() != fluid.getFluid()) {
			FluidReactions.handlePipeSpillCollision(world, outputPos, fluid.getFluid(), fluidState);
			return false;
		}

		if (fluidState.isSource())
			return false;
		if (waterlog && fluid.getFluid() != Fluids.WATER)
			return false;
		if (simulate)
			return true;

		if (world.dimensionType()
			.ultraWarm()
			&& fluid.getFluid()
				.is(FluidTags.WATER)) {
			int i = outputPos.getX();
			int j = outputPos.getY();
			int k = outputPos.getZ();
			world.playSound(null, i, j, k, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
				2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
			return true;
		}

		AllTriggers.triggerForNearbyPlayers(AllTriggers.PIPE_SPILL, world, pos, 5);

		if (waterlog) {
			world.setBlock(outputPos, state.setValue(WATERLOGGED, true), 3);
			world.scheduleTick(outputPos, Fluids.WATER, 1);
			return true;
		}

		if (!AllConfigs.SERVER.fluids.placeFluidSourceBlocks.get())
			return true;

		world.setBlock(outputPos, fluid.getFluid()
			.defaultFluidState()
			.createLegacyBlock(), 3);
		return true;
	}

	private boolean canApplyEffects(FluidStack fluid) {
		for (IEffectHandler handler : EFFECT_HANDLERS) {
			if (handler.canApplyEffects(this, fluid)) {
				return true;
			}
		}
		return false;
	}

	private void applyEffects(FluidStack fluid) {
		for (IEffectHandler handler : EFFECT_HANDLERS) {
			if (handler.canApplyEffects(this, fluid)) {
				handler.applyEffects(this, fluid);
			}
		}
	}

	private class OpenEndFluidHandler extends FluidTank {

		public OpenEndFluidHandler() {
			super(FluidConstants.BUCKET);
		}

		@Override
		public long fill(FluidStack resource, boolean sim) {
			// Never allow being filled when a source is attached
			if (world == null)
				return 0;
			if (!LevelUtil.isAreaLoaded(world, outputPos, 0))
				return 0;
			if (resource.isEmpty())
				return 0;
			if (!provideFluidToSpace(resource, true))
				return 0;

			FluidStack containedFluidStack = getFluid();
			if (!containedFluidStack.isEmpty() && !containedFluidStack.isFluidEqual(resource))
				setFluid(FluidStack.EMPTY);
			if (wasPulling)
				wasPulling = false;
			if (canApplyEffects(resource))
				resource = FluidHelper.copyStackWithAmount(resource, 81); // fabric: deplete fluids 81 times faster to account for larger amounts

			long fill = super.fill(resource, sim);
			if (sim)
				return fill;
			if (getFluidAmount() == FluidConstants.BUCKET || (!FluidHelper.hasBlockState(containedFluidStack.getFluid()) || containedFluidStack.getFluid().is(Milk.MILK_TAG))) // fabric: milk logic is different
				if (provideFluidToSpace(containedFluidStack, false))
					setFluid(FluidStack.EMPTY);
			return fill;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean sim) {
			return drainInner(resource.getAmount(), resource, sim);
		}

		@Override
		public FluidStack drain(long maxDrain, boolean sim) {
			return drainInner(maxDrain, null, sim);
		}

		private FluidStack drainInner(long amount, @Nullable FluidStack filter, boolean sim) {
			FluidStack empty = FluidStack.EMPTY;
			boolean filterPresent = filter != null;

			if (world == null)
				return empty;
			if (!LevelUtil.isAreaLoaded(world, outputPos, 0))
				return empty;
			if (amount == 0)
				return empty;
			if (amount > FluidConstants.BUCKET) {
				amount = FluidConstants.BUCKET;
				if (filterPresent)
					filter = FluidHelper.copyStackWithAmount(filter, amount);
			}

			if (!wasPulling)
				wasPulling = true;

			FluidStack drainedFromInternal = filterPresent ? super.drain(filter, sim) : super.drain(amount, sim);
			if (!drainedFromInternal.isEmpty())
				return drainedFromInternal;

			FluidStack drainedFromWorld = removeFluidFromSpace(sim);
			if (drainedFromWorld.isEmpty())
				return FluidStack.EMPTY;
			if (filterPresent && !drainedFromWorld.isFluidEqual(filter))
				return FluidStack.EMPTY;

			long remainder = drainedFromWorld.getAmount() - amount;
			drainedFromWorld.setAmount(amount);

			if (!sim && remainder > 0) {
				if (!getFluid().isEmpty() && !getFluid().isFluidEqual(drainedFromWorld))
					setFluid(FluidStack.EMPTY);
				super.fill(FluidHelper.copyStackWithAmount(drainedFromWorld, remainder), false);
			}
			return drainedFromWorld;
		}

	}

	public interface IEffectHandler {
		boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid);

		void applyEffects(OpenEndedPipe pipe, FluidStack fluid);
	}

	public static class PotionEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid().isSame(AllFluids.POTION.get());
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			if (pipe.cachedFluid == null || pipe.cachedEffects == null || !fluid.isFluidEqual(pipe.cachedFluid)) {
				FluidStack copy = fluid.copy();
				copy.setAmount(FluidConstants.BOTTLE);
				ItemStack bottle = PotionFluidHandler.fillBottle(new ItemStack(Items.GLASS_BOTTLE), fluid);
				pipe.cachedEffects = PotionUtils.getMobEffects(bottle);
			}

			if (pipe.cachedEffects.isEmpty())
				return;

			List<LivingEntity> entities =
				pipe.getWorld().getEntitiesOfClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedByPotions);
			for (LivingEntity entity : entities) {
				for (MobEffectInstance effectInstance : pipe.cachedEffects) {
					MobEffect effect = effectInstance.getEffect();
					if (effect.isInstantenous()) {
						effect.applyInstantenousEffect(null, null, entity, effectInstance.getAmplifier(), 0.5D);
					} else {
						entity.addEffect(new MobEffectInstance(effectInstance));
					}
				}
			}
		}
	}

	public static class MilkEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid().is(Tags.Fluids.MILK);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<LivingEntity> entities =
				world.getEntitiesOfClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedByPotions);
//			ItemStack curativeItem = new ItemStack(Items.MILK_BUCKET);
			for (LivingEntity entity : entities)
				entity.removeAllEffects();
		}
	}

	public static class WaterEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid().is(FluidTags.WATER);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<Entity> entities = world.getEntities((Entity) null, pipe.getAOE(), Entity::isOnFire);
			for (Entity entity : entities)
				entity.clearFire();
			BlockPos.betweenClosedStream(pipe.getAOE()).forEach(pos -> dowseFire(world, pos));
		}

		// Adapted from ThrownPotion
		private static void dowseFire(Level level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.FIRE)) {
				level.removeBlock(pos, false);
			} else if (AbstractCandleBlock.isLit(state)) {
				AbstractCandleBlock.extinguish(null, state, level, pos);
			} else if (CampfireBlock.isLitCampfire(state)) {
				level.levelEvent(null, 1009, pos, 0);
				CampfireBlock.dowse(null, level, pos, state);
				level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
			}
		}
	}

	public static class LavaEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid().is(FluidTags.LAVA);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<Entity> entities = world.getEntities((Entity) null, pipe.getAOE(), entity -> !entity.fireImmune());
			for (Entity entity : entities)
				entity.setSecondsOnFire(3);
		}
	}

}
