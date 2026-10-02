package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.block.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Palimpsest.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ScriptoriumDeskBlockEntity>> SCRIPTORIUM_DESK = BLOCK_ENTITIES.register("scriptorium_desk",
            () -> BlockEntityType.Builder.of(ScriptoriumDeskBlockEntity::new, ModBlocks.SCRIPTORIUM_DESK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<RubricAltarBlockEntity>> RUBRIC_ALTAR = BLOCK_ENTITIES.register("rubric_altar",
            () -> BlockEntityType.Builder.of(RubricAltarBlockEntity::new, ModBlocks.RUBRIC_ALTAR.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ReadingStandBlockEntity>> READING_STAND = BLOCK_ENTITIES.register("reading_stand",
            () -> BlockEntityType.Builder.of(ReadingStandBlockEntity::new, ModBlocks.READING_STAND.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BlankBlockEntity>> BLANK = BLOCK_ENTITIES.register("blank",
            () -> BlockEntityType.Builder.of(BlankBlockEntity::new, ModBlocks.BLANK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TearBlockEntity>> TEAR = BLOCK_ENTITIES.register("tear",
            () -> BlockEntityType.Builder.of(TearBlockEntity::new, ModBlocks.TEAR.get()).build(null));

    private ModBlockEntities() {}
}
