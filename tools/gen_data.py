"""Writes every JSON asset and data file (blockstates, models, loot, recipes,
tags, advancements, worldgen, language). Deterministic; safe to re-run."""
import data_advancements
import data_blocks
import data_items
import data_lang
import data_loot
import data_recipes
import data_tags
import data_worldgen

if __name__ == "__main__":
    for mod in (data_blocks, data_items, data_loot, data_recipes, data_tags,
                data_advancements, data_worldgen, data_lang):
        mod.gen()
        print("ok", mod.__name__)
