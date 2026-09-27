package com.exonoxic.palimpsest.bleed;

/** What a player chose at the Folio Stand after the Rasure fell. */
public enum Ending {
    NONE,
    /** Wrote their own name over the tear. The page is clean for good. */
    SEALED,
    /** Read the Last Folio aloud. The First Draft stays open around them forever. */
    READ,
    /** Scraped their own name away. The inkborn no longer know they exist. */
    UNWRITTEN;

    public static Ending byOrdinal(int i) {
        Ending[] v = values();
        return i >= 0 && i < v.length ? v[i] : NONE;
    }
}
