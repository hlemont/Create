package com.simibubi.create.modules.contraptions.relays.belt;

import static net.minecraft.block.Block.makeCuboidShape;

import com.simibubi.create.foundation.utility.AllShapes;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.simibubi.create.modules.contraptions.relays.belt.BeltBlock.Part;
import com.simibubi.create.modules.contraptions.relays.belt.BeltBlock.Slope;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

public class BeltShapes {

	/*
	 * | hi. i made these comments mostly to help me with creating the shapes. but they should also be able to help you understand what i'm doing here if that's why you came here. cheers
	 * |
	 * |                 belt shape slope south descending
	 * |                    generated by makeSlopePart
	 * |         z
	 * |  y        15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0   |               |                                                    |                                                           |
	 * |            |                                            |   |               |                                                    |                                                           |
	 * | +5         |                                            #   |               |            belt shape flat south ending            |            belt shape flat south full                     |
	 * | +4         |                                         #  #   |               |             generated by makeFlatEnding            |             generated by makeFlatFull                     |
	 * | +3         |                                      #  #  #   | z             |z                                                   |z                                                          |
	 * | +2         |                                   #  #  #  #   |  15  14 ...   |  15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0   |  15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0          |
	 * | +1         |                                #  #  #  #  #   |   |           |   |                                            |   |   |                                            |          |
	 * | 15   ------+  -  -  -  -  -  -  -  -  -  #  #  #  #  #  #-- | --+---------- | --+  -  -  -  -  -  -  -  -  -  -  -  -  -  -  +-- | --+  -  -  -  -  -  -  -  -  -  -  -  -  -  -  +---       |
	 * | 14         |                          #  #  #  #  #  #  #   |   |           |   |                                            |   |   |                                            |          |
	 * | 13         |                       #  #  #  #  #  #  #  #   |   |           |   |                                            |   |   |                                            |          |
	 * | 12         |                    #  #  #  #  #  #  #  #  #   |   |           |   |  #  #  #  #  #  #  #  #  #  #  #  #  #  #  |   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 11         |                 #  #  #  #  #  #  #  #  #  #   |   |           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 10         |              #  #  #  #  #  #  #  #  #  #  #   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 9          |           #  #  #  #  #  #  #  #  #  #  #  |   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 8          |        #  #  #  #  #  #  #  #  #  #  #     |   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 7          |     #  #  #  #  #  #  #  #  #  #  #        |   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 6          |  #  #  #  #  #  #  #  #  #  #  #           |   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 5          #  #  #  #  #  #  #  #  #  #  #              |   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 4          #  #  #  #  #  #  #  #  #  #                 |   |   #           |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 3          #  #  #  #  #  #  #  #  #                    |   |   #           |   |  #  #  #  #  #  #  #  #  #  #  #  #  #  #  |   |   #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #          |
	 * | 2          #  #  #  #  #  #  #  #                       |   |   #           |   |                                            |   |   |                                            |          |
	 * | 1          #  #  #  #  #  #  #                          |   |   #           |   |                                            |   |   |                                            |          |
	 * | 0   -------#  #  #  #  #  #  -  -  -  -  -  -  -  -  -  +-- | --#---------- | --+  -  -  -  -  -  -  -  -  -  -  -  -  -  -  +-- | --+  -  -  -  -  -  -  -  -  -  -  -  -  -  -  +---       |
	 * |-1          #  #  #  #  #                                |   |   |           |   |                                            |   |   |                                            |          |
	 * |-2          #  #  #  #                                   |   |   |           |   |                                            |   |   |                                            |          |
	 * |-3          #  #  #                                          |  slice used   |                                                    |                                                           |
	 * |-4          #  #                                             |  to create    |                                                    |                                                           |
	 * |-5          #                                                |  the stairs   |                                                    |                                                           |
	 * |                                  x 1 to 14                  |               |                                                    |                                                           |
	 */

	/*
	 * |Belt shapes always consist of 2 halves depending on state. This class generated all shapes for belts facing SOUTH and then uses VoxelShapers to fill the remaining 3 Directions
	 * |Middle shapes use the same building part in both halved and don't need to be composed
	 * |some of these shapes could be skipped and easily achieved by rotating other shapes but i left them in for clarity's sake
	 * | Flat Belts:                                    Sloped Belts: (DESC)                           (ASC)
	 * |                south half    north half                         south half    north half         south half    north half
	 * |
	 * |      Middle     flat full     flat full               Middle    slope desc    slope desc          slope asc     slope asc
	 * |         End      flat end     flat full                  End     flat end     slope desc           flat end     slope asc
	 * |       Start     flat full      flat end                Start    slope desc     flat end           slope asc      flat end
	 */

	//Building parts for the shapes
	private static final VoxelShape
			SLOPE_DESC_PART = makeSlopePart(false),
			SLOPE_ASC_PART = makeSlopePart(true),
			FLAT_FULL_PART = makeFlatFull(),
			FLAT_END_PART = makeFlatEnding();

	private static final VoxelShape SOUTH_MASK = makeCuboidShape(0,-5,8,16,16+5,16);
	private static final VoxelShape NORTH_MASK = makeCuboidShape(0,-5,0,16,16+5,8);

	//Vertical Shapes
	private static final VoxelShaper
			VERTICAL_FULL = VerticalBeltShaper.make(FLAT_FULL_PART),
			VERTICAL_END = VerticalBeltShaper.make(compose(FLAT_END_PART, FLAT_FULL_PART)),
			VERTICAL_START = VerticalBeltShaper.make(compose(FLAT_FULL_PART, FLAT_END_PART));
	//Flat Shapes
	private static final VoxelShaper
			FLAT_FULL = VoxelShaper.forHorizontalAxis(FLAT_FULL_PART, Axis.Z),
			FLAT_END = VoxelShaper.forHorizontal(compose(FLAT_END_PART, FLAT_FULL_PART), Direction.SOUTH),
			FLAT_START = VoxelShaper.forHorizontal(compose(FLAT_FULL_PART, FLAT_END_PART), Direction.SOUTH);
	//Sloped Shapes
	private static final VoxelShaper
			SLOPE_DESC = VoxelShaper.forHorizontal(SLOPE_DESC_PART, Direction.SOUTH),
			SLOPE_ASC = VoxelShaper.forHorizontal(SLOPE_ASC_PART, Direction.SOUTH),
			SLOPE_DESC_END = VoxelShaper.forHorizontal(compose(FLAT_END_PART, SLOPE_DESC_PART), Direction.SOUTH),
			SLOPE_DESC_START = VoxelShaper.forHorizontal(compose(SLOPE_DESC_PART, FLAT_END_PART), Direction.SOUTH),
			SLOPE_ASC_END = VoxelShaper.forHorizontal(compose(FLAT_END_PART, SLOPE_ASC_PART), Direction.SOUTH),
			SLOPE_ASC_START = VoxelShaper.forHorizontal(compose(SLOPE_ASC_PART, FLAT_END_PART), Direction.SOUTH);

	private static final VoxelShaper
			PARTIAL_CASING = VoxelShaper.forHorizontal(makeCuboidShape(0, 0, 5, 16, 11, 16), Direction.SOUTH);


	private static VoxelShape compose(VoxelShape southPart, VoxelShape northPart){
		return VoxelShapes.or(
				VoxelShapes.combine(SOUTH_MASK, southPart, IBooleanFunction.AND),
				VoxelShapes.combine(NORTH_MASK, northPart, IBooleanFunction.AND)
		);
	}

	private static VoxelShape makeSlopePart(boolean ascendingInstead) {
		VoxelShape slice = makeCuboidShape(1, 0, 16, 15, 11, 15);
		VoxelShape result = VoxelShapes.empty();

		for (int i = 0; i < 16; i++) {

			int yOffset = ascendingInstead ? 10 - i : i - 5;

			result = VoxelShapes.or(
					result,//move slice i voxels "right" and i-5 voxels "down"
					slice.withOffset(0, yOffset / 16f, -i / 16f));
		}

		return result;
	}

	private static VoxelShape makeFlatEnding(){
		return VoxelShapes.or(
				makeCuboidShape(1,4,0,15,12,16),
				makeCuboidShape(1,3,1,15,13,15)
		);
	}

	private static VoxelShape makeFlatFull(){
		return makeCuboidShape(1,3,0,15,13,16);
	}

	public static VoxelShape getShape(BlockState state) {
		Direction facing = state.get(BeltBlock.HORIZONTAL_FACING);
		Axis axis = facing.getAxis();
		Part part = state.get(BeltBlock.PART);
		Slope slope = state.get(BeltBlock.SLOPE);

		//vertical
		if (slope == Slope.VERTICAL) {
			if (part == Part.MIDDLE || part == Part.PULLEY)
				return VERTICAL_FULL.get(axis);
			//vertical ending
			return (part == Part.START ? VERTICAL_START : VERTICAL_END).get(facing);
		}

		//flat part
		if (slope == Slope.HORIZONTAL) {
			if (part == Part.MIDDLE || part == Part.PULLEY)
				return FLAT_FULL.get(axis);
			//flat ending
			return (part == Part.START ? FLAT_START : FLAT_END).get(facing);
		}

		//slope
		if (part == Part.MIDDLE || part == Part.PULLEY)
			return (slope == Slope.DOWNWARD ? SLOPE_DESC : SLOPE_ASC).get(facing);
		//sloped ending
		if (part == Part.START)
			return (slope == Slope.DOWNWARD ? SLOPE_DESC_START : SLOPE_ASC_START).get(facing);
		if (part == Part.END)
			return (slope == Slope.DOWNWARD ? SLOPE_DESC_END : SLOPE_ASC_END).get(facing);

		//bad state
		return VoxelShapes.empty();

	}

	public static VoxelShape getCasingShape(BlockState state) {
		if (!state.get(BeltBlock.CASING))
			return VoxelShapes.empty();

		Direction facing = state.get(BeltBlock.HORIZONTAL_FACING);
		Part part = state.get(BeltBlock.PART);
		Slope slope = state.get(BeltBlock.SLOPE);

		if (slope == Slope.VERTICAL)
			return VoxelShapes.empty();

		if (slope == Slope.HORIZONTAL) {
			return AllShapes.SHORT_CASING_11_VOXEL.get(Direction.UP);
		}

		if (part == Part.MIDDLE || part == Part.PULLEY)
			return PARTIAL_CASING.get(slope == Slope.UPWARD ? facing : facing.getOpposite());

		if (part == Part.START)
			return slope == Slope.UPWARD ? AllShapes.SHORT_CASING_11_VOXEL.get(Direction.UP) : PARTIAL_CASING.get(facing.getOpposite());
		if (part == Part.END)
			return slope == Slope.DOWNWARD ? AllShapes.SHORT_CASING_11_VOXEL.get(Direction.UP) : PARTIAL_CASING.get(facing);

		//something went wrong
		return VoxelShapes.fullCube();
	}

	private static class VerticalBeltShaper extends VoxelShaper {

		public static VoxelShaper make(VoxelShape southBeltShape){
			return forDirectionsWithRotation(
					rotatedCopy(southBeltShape, new Vec3d(-90,0,0)),
					Direction.SOUTH,
					Direction.Plane.HORIZONTAL,//idk, this can probably be improved :S
					direction -> new Vec3d(
							direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 180,
							-direction.getHorizontalAngle(),
							0));
		}
	}

}
