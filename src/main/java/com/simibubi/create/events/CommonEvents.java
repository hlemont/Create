package com.simibubi.create.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import com.simibubi.create.content.contraptions.fluids.FluidBottleItemHook;

import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerHandler;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.tileEntity.behaviour.edgeInteraction.EdgeInteractionHandler;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringHandler;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkHandler;

import com.simibubi.create.foundation.utility.fabric.AbstractMinecartExtensions;

import io.github.fabricators_of_create.porting_lib.event.EntityEvents;
import io.github.fabricators_of_create.porting_lib.event.EntityReadExtraDataCallback;
import io.github.fabricators_of_create.porting_lib.event.MinecartEvents;
import io.github.fabricators_of_create.porting_lib.event.ProjectileImpactCallback;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.simibubi.create.AllFluids;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.components.crusher.CrushingWheelTileEntity;
import com.simibubi.create.content.contraptions.components.deployer.DeployerFakePlayer;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.MinecartContraptionItem;
import com.simibubi.create.content.contraptions.components.structureMovement.train.CouplingHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.train.CouplingPhysics;
import com.simibubi.create.content.contraptions.components.structureMovement.train.MinecartCouplingItem;
import com.simibubi.create.content.contraptions.components.structureMovement.train.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.fluids.recipe.FluidTransferRecipes;
import com.simibubi.create.content.contraptions.fluids.recipe.PotionMixingRecipeManager;
import com.simibubi.create.content.contraptions.wrench.WrenchItem;
import com.simibubi.create.content.curiosities.armor.DivingBootsItem;
import com.simibubi.create.content.curiosities.armor.DivingHelmetItem;
import com.simibubi.create.content.curiosities.bell.HauntedBellPulser;
import com.simibubi.create.content.curiosities.symmetry.SymmetryHandler;
import com.simibubi.create.content.curiosities.toolbox.ToolboxHandler;
import com.simibubi.create.content.curiosities.tools.ExtendoGripItem;
import com.simibubi.create.content.curiosities.weapons.PotatoProjectileTypeManager;
import com.simibubi.create.content.curiosities.zapper.ZapperInteractionHandler;
import com.simibubi.create.content.curiosities.zapper.ZapperItem;
import com.simibubi.create.content.logistics.item.LinkedControllerServerHandler;
import com.simibubi.create.foundation.command.AllCommands;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.WorldAttached;
import com.simibubi.create.foundation.utility.recipe.RecipeFinder;
import com.simibubi.create.foundation.worldgen.AllWorldFeatures;
import io.github.fabricators_of_create.porting_lib.event.BlockPlaceCallback;
import io.github.fabricators_of_create.porting_lib.event.DataPackReloadCallback;
import io.github.fabricators_of_create.porting_lib.event.FluidPlaceBlockCallback;
import io.github.fabricators_of_create.porting_lib.event.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.event.MobEntitySetTargetCallback;
import io.github.fabricators_of_create.porting_lib.event.OnDatapackSyncCallback;
import io.github.fabricators_of_create.porting_lib.event.ServerPlayerCreationCallback;
import io.github.fabricators_of_create.porting_lib.event.StartRidingCallback;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.EntityHitResult;

public class CommonEvents {

	public static void onServerTick(MinecraftServer server) {
		Create.SCHEMATIC_RECEIVER.tick();
		Create.LAGGER.tick();
		ServerSpeedProvider.serverTick(server);
	}

	public static void onChunkUnloaded(Level world, LevelChunk chunk) {
		CapabilityMinecartController.onChunkUnloaded(world, chunk);
	}

	public static void playerLoggedIn(ServerPlayer player) {
		ToolboxHandler.playerLogin(player);
	}

	public static BlockState whenFluidsMeet(LevelAccessor world, BlockPos pos, BlockState blockState) {
		FluidState fluidState = blockState.getFluidState();

		if (fluidState.isSource() && FluidHelper.isLava(fluidState.getType()))
			return null;

		for (Direction direction : Iterate.directions) {
			FluidState metFluidState =
				fluidState.isSource() ? fluidState : world.getFluidState(pos.relative(direction));
			if (!metFluidState.is(FluidTags.WATER))
				continue;
			BlockState lavaInteraction = AllFluids.getLavaInteraction(metFluidState);
			if (lavaInteraction == null)
				continue;
			return lavaInteraction;
		}
		return null;
	}

	public static void onWorldTick(Level world) {
		// on forge, this is only called on ServerLevels
		if (!world.isClientSide()) {
			ContraptionHandler.tick(world);
			CapabilityMinecartController.tick(world);
			CouplingPhysics.tick(world);
			LinkedControllerServerHandler.tick(world);
		}
	}

	public static void onUpdateLivingEntity(LivingEntity entityLiving) {
		Level world = entityLiving.level;
		if (world == null)
			return;
		ContraptionHandler.entitiesWhoJustDismountedGetSentToTheRightLocation(entityLiving, world);
		ToolboxHandler.entityTick(entityLiving, world);
	}

	public static void onEntityAdded(Entity entity, Level world) {
		ContraptionHandler.addSpawnedContraptionsToCollisionList(entity, world);
	}

	public static InteractionResult onEntityAttackedByPlayer(Player playerEntity, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult entityRayTraceResult) {
		return WrenchItem.wrenchInstaKillsMinecarts(playerEntity, world, hand, entity, entityRayTraceResult);
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
		AllCommands.register(dispatcher);
	}

	public static List<PreparableReloadListener> addReloadListeners(ServerResources dataPackRegistries) {
		List<PreparableReloadListener> listeners = new ArrayList<>();
		listeners.add(RecipeFinder.LISTENER);
		listeners.add(PotionMixingRecipeManager.LISTENER);
		listeners.add(FluidTransferRecipes.LISTENER);
		listeners.add(PotatoProjectileTypeManager.ReloadListener.INSTANCE);
		return listeners;
	}

	public static void onDatapackSync(PlayerList playerList, @javax.annotation.Nullable ServerPlayer player) {
		if (player != null) {
			PotatoProjectileTypeManager.syncTo(player);
		} else {
			PotatoProjectileTypeManager.syncToAll(playerList.getPlayers());
		}
	}

	public static void serverStopping(MinecraftServer server) {
		Create.SCHEMATIC_RECEIVER.shutdown();
	}

	public static void onLoadWorld(Executor executor, LevelAccessor world) {
		Create.REDSTONE_LINK_NETWORK_HANDLER.onLoadWorld(world);
		Create.TORQUE_PROPAGATOR.onLoadWorld(world);
	}

	public static void onUnloadWorld(Executor executor, LevelAccessor world) {
		Create.REDSTONE_LINK_NETWORK_HANDLER.onUnloadWorld(world);
		Create.TORQUE_PROPAGATOR.onUnloadWorld(world);
		WorldAttached.invalidateWorld(world);
	}

	// handled by AbstractMinecartMixin
	public static void attachCapabilities(AbstractMinecart cart) {
		CapabilityMinecartController.attach(cart);
	}

	public static void startTracking(Entity target, ServerPlayer player) {
		CapabilityMinecartController.startTracking(target);
	}

	public static void onBiomeLoad() {
		AllWorldFeatures.reload();
	}

	public static void leftClickEmpty(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof ZapperItem) {
			ZapperInteractionHandler.trySelect(stack, player);
		}
	}

	public static class ModBusEvents {

//		@SubscribeEvent
//		public static void registerCapabilities(RegisterCapabilitiesEvent event) {
//			event.register(CapabilityMinecartController.class);
//		}

	}

	public static void register() {
		// Fabric Events
		ServerTickEvents.END_SERVER_TICK.register(CommonEvents::onServerTick);
		ServerChunkEvents.CHUNK_UNLOAD.register(CommonEvents::onChunkUnloaded);
		ServerTickEvents.END_WORLD_TICK.register(CommonEvents::onWorldTick);
		ServerEntityEvents.ENTITY_LOAD.register(CommonEvents::onEntityAdded);
		ServerLifecycleEvents.SERVER_STOPPED.register(CommonEvents::serverStopping);
		ServerWorldEvents.LOAD.register(CommonEvents::onLoadWorld);
		ServerWorldEvents.UNLOAD.register(CommonEvents::onUnloadWorld);
		ServerTickEvents.END_WORLD_TICK.register(HauntedBellPulser::hauntedBellCreatesPulse);
		PlayerBlockBreakEvents.AFTER.register(SymmetryHandler::onBlockDestroyed);
		AttackBlockCallback.EVENT.register(ZapperInteractionHandler::leftClickingBlocksWithTheZapperSelectsTheBlock);
		AttackEntityCallback.EVENT.register(CommonEvents::onEntityAttackedByPlayer);
		UseEntityCallback.EVENT.register(MinecartCouplingItem::handleInteractionWithMinecart);
		UseEntityCallback.EVENT.register(MinecartContraptionItem::wrenchCanBeUsedToPickUpMinecartContraptions);
		UseBlockCallback.EVENT.register(FilteringHandler::onBlockActivated);
		UseBlockCallback.EVENT.register(LinkHandler::onBlockActivated);
		UseBlockCallback.EVENT.register(ItemUseOverrides::onBlockActivated);
		UseBlockCallback.EVENT.register(EdgeInteractionHandler::onBlockActivated);
		UseBlockCallback.EVENT.register(FluidBottleItemHook::preventWaterBottlesFromCreatesFluids);
		CommandRegistrationCallback.EVENT.register(CommonEvents::registerCommands);
		// Fabric Biome API requires biomes to only be registered once
		CommonEvents.onBiomeLoad();

		// External Events

		LivingEntityEvents.TICK.register(CommonEvents::onUpdateLivingEntity);
		DataPackReloadCallback.EVENT.register(CommonEvents::addReloadListeners);
		ServerPlayerCreationCallback.EVENT.register(CommonEvents::playerLoggedIn);
		FluidPlaceBlockCallback.EVENT.register(CommonEvents::whenFluidsMeet);
		OnDatapackSyncCallback.EVENT.register(CommonEvents::onDatapackSync);
		MobEntitySetTargetCallback.EVENT.register(DeployerFakePlayer::entitiesDontRetaliate);
		StartRidingCallback.EVENT.register(CouplingHandler::preventEntitiesFromMoutingOccupiedCart);
		LivingEntityEvents.EXPERIENCE_DROP.register(DeployerFakePlayer::deployerKillsDoNotSpawnXP);
		LivingEntityEvents.HURT.register(ExtendoGripItem::bufferLivingAttackEvent);
		LivingEntityEvents.KNOCKBACK_STRENGTH.register(ExtendoGripItem::attacksByExtendoGripHaveMoreKnockback);
		LivingEntityEvents.TICK.register(ExtendoGripItem::holdingExtendoGripIncreasesRange);
		LivingEntityEvents.TICK.register(DivingBootsItem::accellerateDescentUnderwater);
		LivingEntityEvents.TICK.register(DivingHelmetItem::breatheUnderwater);
		LivingEntityEvents.DROPS.register(CrushingWheelTileEntity::handleCrushedMobDrops);
		LivingEntityEvents.LOOTING_LEVEL.register(CrushingWheelTileEntity::crushingIsFortunate);
		LivingEntityEvents.DROPS.register(DeployerFakePlayer::deployerCollectsDropsFromKilledEntities);
		EntityEvents.EYE_HEIGHT.register(DeployerFakePlayer::deployerHasEyesOnHisFeet);
		BlockPlaceCallback.EVENT.register(SymmetryHandler::onBlockPlaced);
		BlockPlaceCallback.EVENT.register(SuperGlueHandler::glueListensForBlockPlacement);
		ProjectileImpactCallback.EVENT.register(BlazeBurnerHandler::onThrowableImpact);
		EntityReadExtraDataCallback.EVENT.register(ExtendoGripItem::addReachToJoiningPlayersHoldingExtendo);
		MinecartEvents.SPAWN.register(AbstractMinecartExtensions::minecartSpawn);
		MinecartEvents.READ.register(AbstractMinecartExtensions::minecartRead);
		MinecartEvents.WRITE.register(AbstractMinecartExtensions::minecartWrite);
		MinecartEvents.REMOVE.register(AbstractMinecartExtensions::minecartRemove);
	}

}
