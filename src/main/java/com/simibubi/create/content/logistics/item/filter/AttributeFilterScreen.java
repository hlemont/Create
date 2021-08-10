package com.simibubi.create.content.logistics.item.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.item.filter.AttributeFilterContainer.WhitelistMode;
import com.simibubi.create.content.logistics.item.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widgets.IconButton;
import com.simibubi.create.foundation.gui.widgets.Indicator;
import com.simibubi.create.foundation.gui.widgets.Label;
import com.simibubi.create.foundation.gui.widgets.SelectionScrollInput;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

public class AttributeFilterScreen extends AbstractFilterScreen<AttributeFilterContainer> {

	private static final String PREFIX = "gui.attribute_filter.";

	private IconButton whitelistDis, whitelistCon, blacklist;
	private Indicator whitelistDisIndicator, whitelistConIndicator, blacklistIndicator;
	private IconButton add;
	private IconButton addInverted;

	private Component addDESC = Lang.translate(PREFIX + "add_attribute");
	private Component addInvertedDESC = Lang.translate(PREFIX + "add_inverted_attribute");

	private Component allowDisN = Lang.translate(PREFIX + "allow_list_disjunctive");
	private Component allowDisDESC = Lang.translate(PREFIX + "allow_list_disjunctive.description");
	private Component allowConN = Lang.translate(PREFIX + "allow_list_conjunctive");
	private Component allowConDESC = Lang.translate(PREFIX + "allow_list_conjunctive.description");
	private Component denyN = Lang.translate(PREFIX + "deny_list");
	private Component denyDESC = Lang.translate(PREFIX + "deny_list.description");

	private Component referenceH = Lang.translate(PREFIX + "add_reference_item");
	private Component noSelectedT = Lang.translate(PREFIX + "no_selected_attributes");
	private Component selectedT = Lang.translate(PREFIX + "selected_attributes");

	private ItemStack lastItemScanned = ItemStack.EMPTY;
	private List<ItemAttribute> attributesOfItem = new ArrayList<>();
	private List<Component> selectedAttributes = new ArrayList<>();
	private SelectionScrollInput attributeSelector;
	private Label attributeSelectorLabel;

	public AttributeFilterScreen(AttributeFilterContainer container, Inventory inv, Component title) {
		super(container, inv, title, AllGuiTextures.ATTRIBUTE_FILTER);
	}

	@Override
	protected void init() {
		setWindowOffset(-11, 7);
		super.init();

		int x = leftPos;
		int y = topPos;

		whitelistDis = new IconButton(x + 47, y + 59, AllIcons.I_WHITELIST_OR);
		whitelistDis.setToolTip(allowDisN);
		whitelistCon = new IconButton(x + 65, y + 59, AllIcons.I_WHITELIST_AND);
		whitelistCon.setToolTip(allowConN);
		blacklist = new IconButton(x + 83, y + 59, AllIcons.I_WHITELIST_NOT);
		blacklist.setToolTip(denyN);

		whitelistDisIndicator = new Indicator(x + 47, y + 53, TextComponent.EMPTY);
		whitelistConIndicator = new Indicator(x + 65, y + 53, TextComponent.EMPTY);
		blacklistIndicator = new Indicator(x + 83, y + 53, TextComponent.EMPTY);

		widgets.addAll(Arrays.asList(blacklist, whitelistCon, whitelistDis, blacklistIndicator, whitelistConIndicator,
			whitelistDisIndicator));

		widgets.add(add = new IconButton(x + 182, y + 21, AllIcons.I_ADD));
		widgets.add(addInverted = new IconButton(x + 200, y + 21, AllIcons.I_ADD_INVERTED_ATTRIBUTE));
		add.setToolTip(addDESC);
		addInverted.setToolTip(addInvertedDESC);

		handleIndicators();

		attributeSelectorLabel = new Label(x + 43, y + 26, TextComponent.EMPTY).colored(0xF3EBDE)
			.withShadow();
		attributeSelector = new SelectionScrollInput(x + 39, y + 21, 137, 18);
		attributeSelector.forOptions(List.of(TextComponent.EMPTY));
		attributeSelector.removeCallback();
		referenceItemChanged(menu.ghostInventory.getStackInSlot(0));

		widgets.add(attributeSelector);
		widgets.add(attributeSelectorLabel);

		selectedAttributes.clear();
		selectedAttributes.add((menu.selectedAttributes.isEmpty() ? noSelectedT : selectedT).plainCopy()
			.withStyle(ChatFormatting.YELLOW));
		menu.selectedAttributes.forEach(at -> selectedAttributes.add(new TextComponent("- ")
			.append(at.getFirst()
				.format(at.getSecond()))
			.withStyle(ChatFormatting.GRAY)));
	}

	private void referenceItemChanged(ItemStack stack) {
		lastItemScanned = stack;

		if (stack.isEmpty()) {
			attributeSelector.active = false;
			attributeSelector.visible = false;
			attributeSelectorLabel.text = referenceH.plainCopy()
				.withStyle(ChatFormatting.ITALIC);
			add.active = false;
			addInverted.active = false;
			attributeSelector.calling(s -> {
			});
			return;
		}

		add.active = true;

		addInverted.active = true;
		attributeSelector.titled(stack.getHoverName()
			.plainCopy()
			.append("..."));
		attributesOfItem.clear();
		for (ItemAttribute itemAttribute : ItemAttribute.types)
			attributesOfItem.addAll(itemAttribute.listAttributesOf(stack, minecraft.level));
		List<Component> options = attributesOfItem.stream()
			.map(a -> a.format(false))
			.collect(Collectors.toList());
		attributeSelector.forOptions(options);
		attributeSelector.active = true;
		attributeSelector.visible = true;
		attributeSelector.setState(0);
		attributeSelector.calling(i -> {
			attributeSelectorLabel.setTextAndTrim(options.get(i), true, 112);
			ItemAttribute selected = attributesOfItem.get(i);
			for (Pair<ItemAttribute, Boolean> existing : menu.selectedAttributes) {
				CompoundTag testTag = new CompoundTag();
				CompoundTag testTag2 = new CompoundTag();
				existing.getFirst()
					.serializeNBT(testTag);
				selected.serializeNBT(testTag2);
				if (testTag.equals(testTag2)) {
					add.active = false;
					addInverted.active = false;
					return;
				}
			}
			add.active = true;
			addInverted.active = true;
		});
		attributeSelector.onChanged();
	}

	@Override
	public void renderWindowForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		ItemStack stack = menu.ghostInventory.getStackInSlot(1);
		matrixStack.pushPose();
		matrixStack.translate(0.0F, 0.0F, 32.0F);
		this.setBlitOffset(150);
		this.itemRenderer.blitOffset = 150.0F;
		this.itemRenderer.renderGuiItemDecorations(font, stack, leftPos + 22, topPos + 57,
			String.valueOf(selectedAttributes.size() - 1));
		this.setBlitOffset(0);
		this.itemRenderer.blitOffset = 0.0F;
		matrixStack.popPose();

		super.renderWindowForeground(matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		ItemStack stackInSlot = menu.ghostInventory.getStackInSlot(0);
		if (!ItemStack.matches(stackInSlot, lastItemScanned))
			referenceItemChanged(stackInSlot);
	}

	@Override
	protected void renderTooltip(PoseStack matrixStack, int mouseX, int mouseY) {
		if (this.minecraft.player.getInventory().getSelected()
			.isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
			if (this.hoveredSlot.index == 37) {
				renderComponentTooltip(matrixStack, selectedAttributes, mouseX, mouseY);
				return;
			}
			this.renderTooltip(matrixStack, this.hoveredSlot.getItem(), mouseX, mouseY);
		}
		super.renderTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected List<IconButton> getTooltipButtons() {
		return Arrays.asList(blacklist, whitelistCon, whitelistDis);
	}

	@Override
	protected List<MutableComponent> getTooltipDescriptions() {
		return Arrays.asList(denyDESC.plainCopy(), allowConDESC.plainCopy(), allowDisDESC.plainCopy());
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		boolean mouseClicked = super.mouseClicked(x, y, button);

		if (button != 0)
			return mouseClicked;

		if (blacklist.isHovered()) {
			menu.whitelistMode = WhitelistMode.BLACKLIST;
			sendOptionUpdate(Option.BLACKLIST);
			return true;
		}

		if (whitelistCon.isHovered()) {
			menu.whitelistMode = WhitelistMode.WHITELIST_CONJ;
			sendOptionUpdate(Option.WHITELIST2);
			return true;
		}

		if (whitelistDis.isHovered()) {
			menu.whitelistMode = WhitelistMode.WHITELIST_DISJ;
			sendOptionUpdate(Option.WHITELIST);
			return true;
		}

		if (add.isHovered() && add.active)
			return handleAddedAttibute(false);
		if (addInverted.isHovered() && addInverted.active)
			return handleAddedAttibute(true);

		return mouseClicked;
	}

	protected boolean handleAddedAttibute(boolean inverted) {
		int index = attributeSelector.getState();
		if (index >= attributesOfItem.size())
			return false;
		add.active = false;
		addInverted.active = false;
		CompoundTag tag = new CompoundTag();
		ItemAttribute itemAttribute = attributesOfItem.get(index);
		itemAttribute.serializeNBT(tag);
		AllPackets.channel
			.sendToServer(new FilterScreenPacket(inverted ? Option.ADD_INVERTED_TAG : Option.ADD_TAG, tag));
		menu.appendSelectedAttribute(itemAttribute, inverted);
		if (menu.selectedAttributes.size() == 1)
			selectedAttributes.set(0, selectedT.plainCopy()
				.withStyle(ChatFormatting.YELLOW));
		selectedAttributes.add(new TextComponent("- ").append(itemAttribute.format(inverted))
			.withStyle(ChatFormatting.GRAY));
		return true;
	}

	@Override
	protected void contentsCleared() {
		selectedAttributes.clear();
		selectedAttributes.add(noSelectedT.plainCopy()
			.withStyle(ChatFormatting.YELLOW));
		if (!lastItemScanned.isEmpty()) {
			add.active = true;
			addInverted.active = true;
		}
	}

	@Override
	protected boolean isButtonEnabled(IconButton button) {
		if (button == blacklist)
			return menu.whitelistMode != WhitelistMode.BLACKLIST;
		if (button == whitelistCon)
			return menu.whitelistMode != WhitelistMode.WHITELIST_CONJ;
		if (button == whitelistDis)
			return menu.whitelistMode != WhitelistMode.WHITELIST_DISJ;
		return true;
	}

	@Override
	protected boolean isIndicatorOn(Indicator indicator) {
		if (indicator == blacklistIndicator)
			return menu.whitelistMode == WhitelistMode.BLACKLIST;
		if (indicator == whitelistConIndicator)
			return menu.whitelistMode == WhitelistMode.WHITELIST_CONJ;
		if (indicator == whitelistDisIndicator)
			return menu.whitelistMode == WhitelistMode.WHITELIST_DISJ;
		return false;
	}

}
