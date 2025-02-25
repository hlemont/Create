package com.simibubi.create.content.logistics.item.filter.attribute;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.simibubi.create.content.logistics.item.filter.ItemAttribute;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FireworkStarItem;
import net.minecraft.world.item.ItemStack;

public class ColorAttribute implements ItemAttribute {
	public static final ColorAttribute EMPTY = new ColorAttribute(DyeColor.PURPLE);

	public final DyeColor color;

	public ColorAttribute(DyeColor color) {
		this.color = color;
	}

	@Override
	public boolean appliesTo(ItemStack itemStack) {
		return findMatchingDyeColors(itemStack).stream().anyMatch(color::equals);
	}

	@Override
	public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
		return findMatchingDyeColors(itemStack).stream().map(ColorAttribute::new).collect(Collectors.toList());
	}

	private Collection<DyeColor> findMatchingDyeColors(ItemStack stack) {
		CompoundTag nbt = stack.getTag();

		DyeColor color = TagUtil.getColorFromStack(stack);
		if (color != null)
			return Collections.singletonList(color);

		Set<DyeColor> colors = new HashSet<>();
		if (stack.getItem() instanceof FireworkRocketItem && nbt != null) {
			ListTag listnbt = nbt.getCompound("Fireworks").getList("Explosions", 10);
			for (int i = 0; i < listnbt.size(); i++) {
				colors.addAll(getFireworkStarColors(listnbt.getCompound(i)));
			}
		}

		if (stack.getItem() instanceof FireworkStarItem && nbt != null) {
			colors.addAll(getFireworkStarColors(nbt.getCompound("Explosion")));
		}

		Arrays.stream(DyeColor.values()).filter(c -> Registry.ITEM.getKey(stack.getItem()).getPath().startsWith(c.getName() + "_")).forEach(colors::add);

		return colors;
	}

	private Collection<DyeColor> getFireworkStarColors(CompoundTag compound) {
		Set<DyeColor> colors = new HashSet<>();
		Arrays.stream(compound.getIntArray("Colors")).mapToObj(DyeColor::byFireworkColor).forEach(colors::add);
		Arrays.stream(compound.getIntArray("FadeColors")).mapToObj(DyeColor::byFireworkColor).forEach(colors::add);
		return colors;
	}

	@Override
	public String getTranslationKey() {
		return "color";
	}

	@Override
	public Object[] getTranslationParameters() {
		return new Object[]{new TranslatableComponent(color.getName()).getContents()};
	}

	@Override
	public void writeNBT(CompoundTag nbt) {
		nbt.putInt("id", color.getId());
	}

	@Override
	public ItemAttribute readNBT(CompoundTag nbt) {
		return nbt.contains("id") ?
			new ColorAttribute(DyeColor.byId(nbt.getInt("id")))
			: EMPTY;
	}
}
