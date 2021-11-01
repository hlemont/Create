package com.simibubi.create.content.logistics.item.filter;

import com.simibubi.create.foundation.gui.GhostItemContainer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class AbstractFilterContainer extends GhostItemContainer<ItemStack> {

	protected AbstractFilterContainer(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	protected AbstractFilterContainer(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
		super(type, id, inv, contentHolder);
	}

	@Override
	public ItemStack clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (slotId == playerInventory.selected && clickTypeIn != ClickType.THROW)
			return ItemStack.EMPTY;
		return super.clicked(slotId, dragType, clickTypeIn, player);
	}

	@Override
	protected boolean allowRepeats() {
		return false;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	protected ItemStack createOnClient(FriendlyByteBuf extraData) {
		return extraData.readItem();
	}

	protected abstract int getPlayerInventoryXOffset();

	protected abstract int getPlayerInventoryYOffset();

	protected abstract void addFilterSlots();

	@Override
	protected void addSlots() {
		addPlayerSlots(getPlayerInventoryXOffset(), getPlayerInventoryYOffset());
		addFilterSlots();
	}

	@Override
	protected void saveData(ItemStack contentHolder) {
		contentHolder.getOrCreateTag()
				.put("Items", ghostInventory.serializeNBT());
	}

	@Override
	public boolean stillValid(Player player) {
		return playerInventory.getSelected() == contentHolder;
	}

}
