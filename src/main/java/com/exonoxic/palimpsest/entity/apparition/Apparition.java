package com.exonoxic.palimpsest.entity.apparition;

import java.util.Optional;
import java.util.UUID;

/**
 * Implemented by creatures that can be sent to one player. When apparitions are not shared
 * (config), only that player's client renders them.
 */
public interface Apparition {
    ApparitionState apparition();

    /** Empty when everyone may see this entity. */
    Optional<UUID> exclusiveViewer();
}
