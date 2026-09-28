package com.exonoxic.palimpsest.entity.ai;

/** A creature that can {@link Squeeze} through gaps; its {@link SqueezeNavigation} plans at crawling height. */
public interface Squeezer {
    Squeeze squeeze();
}
