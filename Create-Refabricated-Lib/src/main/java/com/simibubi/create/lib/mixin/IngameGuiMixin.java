package com.simibubi.create.lib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.lib.event.OverlayRenderCallback;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.IngameGui;

@Environment(EnvType.CLIENT)
@Mixin(IngameGui.class)
public abstract class IngameGuiMixin {
	public float create$partialTicks;

	@Inject(at = @At("HEAD"),
			method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V")
	public void create$render(MatrixStack matrixStack, float f, CallbackInfo ci) {
		create$partialTicks = f;
	}

	@Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/profiler/IProfiler;endSection()V"),
			method = "renderStatusBars(Lcom/mojang/blaze3d/matrix/MatrixStack;)V")
	private void create$renderStatusBars(MatrixStack matrixStack, CallbackInfo ci) {
		OverlayRenderCallback.EVENT.invoker().onOverlayRender(matrixStack, create$partialTicks, OverlayRenderCallback.Types.AIR);
	}
}
