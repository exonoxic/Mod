package com.exonoxic.palimpsest.client.model;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/** A plain quadruped over a vanilla layer (the sheep's), usable with any entity type. */
public class GenericQuadrupedModel<T extends Entity> extends QuadrupedModel<T> {
    public GenericQuadrupedModel(ModelPart root, float babyHeadOffset) {
        super(root, false, babyHeadOffset, 4.0F, 2.0F, 2.0F, 24);
    }
}
