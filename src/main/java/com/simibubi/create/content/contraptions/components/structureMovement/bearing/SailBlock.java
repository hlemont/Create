package com.simibubi.create.content.contraptions.components.structureMovement.bearing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.ProperDirectionalBlock;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.placement.PlacementHelpers;
import com.simibubi.create.foundation.utility.placement.PlacementOffset;
import com.simibubi.create.lib.annotation.MethodsReturnNonnullByDefault;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SailBlock extends ProperDirectionalBlock implements BlockPickInteractionAware {

	public static SailBlock frame(Properties properties) {
		return new SailBlock(properties, true);
	}

	public static SailBlock withCanvas(Properties properties) {
		return new SailBlock(properties, false);
	}

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	private final boolean frame;

	protected SailBlock(Properties p_i48415_1_, boolean frame) {
		super(p_i48415_1_);
		this.frame = frame;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state.setValue(FACING, state.getValue(FACING).getOpposite());
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
		ItemStack heldItem = player.getItemInHand(hand);

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (placementHelper.matchesItem(heldItem))
			return placementHelper.getOffset(player, world, state, pos, ray).placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		if (heldItem.getItem() instanceof ShearsItem) {
			if (!world.isClientSide)
				applyDye(state, world, pos, null);
			return InteractionResult.SUCCESS;
		}

		if (frame)
			return InteractionResult.PASS;

		for (DyeColor color : DyeColor.values()) {
			if (!heldItem.getItem()
					.is(DyeHelper.getTagOfDye(color)))
				continue;
			if (!world.isClientSide)
				applyDye(state, world, pos, color);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	protected void applyDye(BlockState state, Level world, BlockPos pos, @Nullable DyeColor color) {
		BlockState newState =
				(color == null ? AllBlocks.SAIL_FRAME : AllBlocks.DYED_SAILS.get(color)).getDefaultState()
						.setValue(FACING, state.getValue(FACING));

		// Dye the block itself
		if (state != newState) {
			world.setBlockAndUpdate(pos, newState);
			return;
		}

		// Dye all adjacent
		for (Direction d : Iterate.directions) {
			if (d.getAxis() == state.getValue(FACING)
					.getAxis())
				continue;
			BlockPos offset = pos.relative(d);
			BlockState adjacentState = world.getBlockState(offset);
			Block block = adjacentState.getBlock();
			if (!(block instanceof SailBlock) || ((SailBlock) block).frame)
				continue;
			if (state == adjacentState)
				continue;
			world.setBlockAndUpdate(offset, newState);
			return;
		}

		// Dye all the things
		List<BlockPos> frontier = new ArrayList<>();
		frontier.add(pos);
		Set<BlockPos> visited = new HashSet<>();
		int timeout = 100;
		while (!frontier.isEmpty()) {
			if (timeout-- < 0)
				break;

			BlockPos currentPos = frontier.remove(0);
			visited.add(currentPos);

			for (Direction d : Iterate.directions) {
				if (d.getAxis() == state.getValue(FACING)
						.getAxis())
					continue;
				BlockPos offset = currentPos.relative(d);
				if (visited.contains(offset))
					continue;
				BlockState adjacentState = world.getBlockState(offset);
				Block block = adjacentState.getBlock();
				if (!(block instanceof SailBlock) || ((SailBlock) block).frame && color != null)
					continue;
				if (state != adjacentState)
					world.setBlockAndUpdate(offset, newState);
				frontier.add(offset);
				visited.add(offset);
			}
		}
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
		return (frame ? AllShapes.SAIL_FRAME : AllShapes.SAIL).get(state.getValue(FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter p_220071_2_, BlockPos p_220071_3_, CollisionContext p_220071_4_) {
		if (frame)
			return AllShapes.SAIL_FRAME_COLLISION.get(state.getValue(FACING));
		return getShape(state, p_220071_2_, p_220071_3_, p_220071_4_);
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockGetter world, BlockPos pos, Player player, HitResult target) {
		ItemStack pickBlock = new ItemStack(world.getBlockState(pos).getBlock().asItem());
		if (pickBlock.isEmpty())
			return AllBlocks.SAIL.get()
					.getPickedStack(state, world, pos, player, target);
		return pickBlock;
	}

	@Override
	public void fallOn(Level level, BlockState blockState, BlockPos blockPos, Entity entity, float f) {
		if (frame)
			super.fallOn(level, blockState, blockPos, entity, f);
		super.fallOn(level, blockState, blockPos, entity, 0);
	}

	public void updateEntityAfterFallOn(BlockGetter p_176216_1_, Entity p_176216_2_) {
		if (frame || p_176216_2_.isSuppressingBounce()) {
			super.updateEntityAfterFallOn(p_176216_1_, p_176216_2_);
		} else {
			this.bounce(p_176216_2_);
		}
	}

	private void bounce(Entity p_226860_1_) {
		Vec3 Vector3d = p_226860_1_.getDeltaMovement();
		if (Vector3d.y < 0.0D) {
			double d0 = p_226860_1_ instanceof LivingEntity ? 1.0D : 0.8D;
			p_226860_1_.setDeltaMovement(Vector3d.x, -Vector3d.y * (double) 0.26F * d0, Vector3d.z);
		}

	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
		return false;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> AllBlocks.SAIL.isIn(i) || AllBlocks.SAIL_FRAME.isIn(i);
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> s.getBlock() instanceof SailBlock;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), state.getValue(SailBlock.FACING).getAxis(), dir -> world.getBlockState(pos.relative(dir)).getMaterial().isReplaceable());

			if (directions.isEmpty())
				return PlacementOffset.fail();
			else {
				return PlacementOffset.success(pos.relative(directions.get(0)), s -> s.setValue(FACING, state.getValue(FACING)));
			}
		}
	}
}
