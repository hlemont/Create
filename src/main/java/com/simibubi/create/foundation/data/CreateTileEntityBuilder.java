package com.simibubi.create.foundation.data;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.backend.instancing.tile.ITileInstanceFactory;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.TileEntityBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.api.EnvType;

public class CreateTileEntityBuilder<T extends BlockEntity, P> extends TileEntityBuilder<T, P> {

	@Nullable
	private NonNullSupplier<ITileInstanceFactory<? super T>> instanceFactory;

	public static <T extends BlockEntity, P> TileEntityBuilder<T, P> create(AbstractRegistrate<?> owner, P parent,
		String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
		return new CreateTileEntityBuilder<>(owner, parent, name, callback, factory);
	}

	protected CreateTileEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
		BlockEntityFactory<T> factory) {
		super(owner, parent, name, callback, factory);
	}

	public CreateTileEntityBuilder<T, P> instance(NonNullSupplier<ITileInstanceFactory<? super T>> instanceFactory) {
		if (this.instanceFactory == null) {
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerInstance);
		}

		this.instanceFactory = instanceFactory;

		return this;
	}

	protected void registerInstance() {
		onRegister(blockEntityType -> InstancedRenderRegistry.getInstance().tile(blockEntityType).factory(instanceFactory.get()));
//		OneTimeEventReceiver.addModListener(FMLClientSetupEvent.class, $ -> {
//			NonNullSupplier<ITileInstanceFactory<? super T>> instanceFactory = this.instanceFactory;
//			if (instanceFactory != null) {
//				InstancedRenderRegistry.getInstance()
//					.tile(getEntry())
//					.factory(instanceFactory.get());
//			}
//		});
	}
}
