package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.bleed.BleedStage;
import com.exonoxic.palimpsest.bleed.Ending;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** What the client knows about its own player's Bleed. Purely for presentation. */
public final class ClientBleedState {
    private static float bleed;
    private static Ending ending = Ending.NONE;
    private static final Set<String> codex = new LinkedHashSet<>();

    public static void set(float value, int endingOrdinal) {
        bleed = value;
        ending = Ending.byOrdinal(endingOrdinal);
    }

    public static void setCodex(List<String> entries) {
        codex.clear();
        codex.addAll(entries);
    }

    public static float bleed() {
        return bleed;
    }

    public static BleedStage stage() {
        return BleedStage.of(bleed);
    }

    public static Ending ending() {
        return ending;
    }

    public static boolean knows(String entry) {
        return codex.contains(entry);
    }

    public static Set<String> codex() {
        return codex;
    }

    /** 0 at the start of the first stage, rising smoothly to 1 at the maximum. */
    public static float severity() {
        return Math.max(0F, Math.min(1F, (bleed - BleedStage.FAINT_TRACE.threshold) / (BleedStage.MAX - BleedStage.FAINT_TRACE.threshold)));
    }

    public static void reset() {
        bleed = 0F;
        ending = Ending.NONE;
        codex.clear();
    }

    private ClientBleedState() {}
}
