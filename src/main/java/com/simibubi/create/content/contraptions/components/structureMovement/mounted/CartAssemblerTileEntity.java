package com.simibubi.create.content.contraptions.components.structureMovement.mounted;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.IDisplayAssemblyExceptions;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.RailShape;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class CartAssemblerTileEntity extends SmartTileEntity implements IDisplayAssemblyExceptions {
	private static final int assemblyCooldown = 8;

	protected ScrollOptionBehaviour<CartMovementMode> movementMode;
	private int ticksSinceMinecartUpdate;
	protected AssemblyException lastException;

	public CartAssemblerTileEntity(TileEntityType<? extends CartAssemblerTileEntity> type) {
		super(type);
		ticksSinceMinecartUpdate = assemblyCooldown;
	}

	@Override
	public void tick() {
		super.tick();
		if (ticksSinceMinecartUpdate < assemblyCooldown) {
			ticksSinceMinecartUpdate++;
		}
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		movementMode = new ScrollOptionBehaviour<>(CartMovementMode.class,
			Lang.translate("contraptions.cart_movement_mode"), this, getMovementModeSlot());
		movementMode.requiresWrench();
		behaviours.add(movementMode);
	}

	@Override
	public void write(CompoundNBT compound, boolean clientPacket) {
		if (lastException != null)
			compound.putString("LastException", ITextComponent.Serializer.toJson(lastException.component));
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(CompoundNBT compound, boolean clientPacket) {
		if (compound.contains("LastException"))
			lastException = new AssemblyException(ITextComponent.Serializer.fromJson(compound.getString("LastException")));
		else
			lastException = null;
		super.read(compound, clientPacket);
	}

	@Override
	public AssemblyException getLastAssemblyException() {
		return lastException;
	}

	protected ValueBoxTransform getMovementModeSlot() {
		return new CartAssemblerValueBoxTransform();
	}

	private class CartAssemblerValueBoxTransform extends CenteredSideValueBoxTransform {
		
		public CartAssemblerValueBoxTransform() {
			super((state, d) -> {
				if (d.getAxis()
					.isVertical())
					return false;
				if (!state.has(CartAssemblerBlock.RAIL_SHAPE))
					return false;
				RailShape railShape = state.get(CartAssemblerBlock.RAIL_SHAPE);
				return (d.getAxis() == Axis.X) == (railShape == RailShape.NORTH_SOUTH);
			});
		}
		
		@Override
		protected Vec3d getSouthLocation() {
			return VecHelper.voxelSpace(8, 8, 18);
		}
		
	}
	
	public static enum CartMovementMode implements INamedIconOptions {

		ROTATE(AllIcons.I_CART_ROTATE),
		ROTATE_PAUSED(AllIcons.I_CART_ROTATE_PAUSED),
		ROTATION_LOCKED(AllIcons.I_CART_ROTATE_LOCKED),

		;

		private String translationKey;
		private AllIcons icon;

		private CartMovementMode(AllIcons icon) {
			this.icon = icon;
			translationKey = "contraptions.cart_movement_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}
	}

	public void resetTicksSinceMinecartUpdate() {
		ticksSinceMinecartUpdate = 0;
	}

	public boolean isMinecartUpdateValid() {
		return ticksSinceMinecartUpdate >= assemblyCooldown;
	}
}
