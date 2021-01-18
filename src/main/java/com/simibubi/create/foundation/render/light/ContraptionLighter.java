package com.simibubi.create.foundation.render.light;

import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.ILightReader;
import net.minecraft.world.LightType;

import static com.simibubi.create.foundation.render.RenderMath.nextPowerOf2;

public abstract class ContraptionLighter<C extends Contraption> {
    protected final C contraption;
    public final LightVolume lightVolume;

    protected ContraptionLighter(C contraption) {
        this.contraption = contraption;

        GridAlignedBB bounds = getContraptionBounds();
        bounds.grow(1); // so we have at least enough data on the edges to avoid artifacts
        GridAlignedBB importantArea = GridAlignedBB.copy(bounds);
        bounds.nextPowerOf2Centered();

        lightVolume = new LightVolume(bounds, importantArea);

        lightVolume.initialize(contraption.entity.world);
    }

    public abstract GridAlignedBB getContraptionBounds();
}
