package com.exonoxic.palimpsest.entity.ai;

/** A creature that can {@link Squeeze} through gaps; its {@link SqueezeNavigation} plans routes for its crawling body. */
public interface Squeezer {
    Squeeze squeeze();
}
