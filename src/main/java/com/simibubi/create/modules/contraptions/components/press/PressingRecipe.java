package com.simibubi.create.modules.contraptions.components.press;

import java.util.List;

import com.simibubi.create.AllRecipes;
import com.simibubi.create.modules.contraptions.components.press.MechanicalPressTileEntity.PressingInv;
import com.simibubi.create.modules.contraptions.processing.ProcessingIngredient;
import com.simibubi.create.modules.contraptions.processing.ProcessingOutput;
import com.simibubi.create.modules.contraptions.processing.ProcessingRecipe;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class PressingRecipe extends ProcessingRecipe<MechanicalPressTileEntity.PressingInv> {

	public PressingRecipe(ResourceLocation id, String group, List<ProcessingIngredient> ingredients,
			List<ProcessingOutput> results, int processingDuration) {
		super(AllRecipes.PRESSING, id, group, ingredients, results, processingDuration);
	}

	@Override
	public boolean matches(PressingInv inv, World worldIn) {
		if (inv.isEmpty())
			return false;
		return ingredients.get(0).test(inv.getStackInSlot(0));
	}
	
	@Override
	protected int getMaxOutputCount() {
		return 2;
	}

}
