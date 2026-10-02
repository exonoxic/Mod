package com.exonoxic.palimpsest.codex;

/**
 * One page of the Commonplace Book. Text lives in the language file under
 * {@code codex.palimpsest.<id>.title} / {@code .text} so it can be translated.
 *
 * @param silent entries that unlock without telling the player (they find them later)
 */
public record CodexEntry(String id, Category category, String icon, boolean silent) {
    public String titleKey() {
        return "codex.palimpsest." + id + ".title";
    }

    public String textKey() {
        return "codex.palimpsest." + id + ".text";
    }

    public enum Category {
        THE_PAGE("the_page"),
        OBSERVATIONS("observations"),
        CREATURES("creatures"),
        PLACES("places"),
        CRAFT("craft"),
        FOLIOS("folios");

        public final String key;

        Category(String key) {
            this.key = key;
        }

        public String titleKey() {
            return "codex.palimpsest.category." + key;
        }
    }
}
