package com.exonoxic.palimpsest.world.structure;

import com.exonoxic.palimpsest.registry.ModStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Wraps another structure (normally a single-piece jigsaw) and refuses to start it where the
 * ground is under water. Vanilla jigsaw structures only check the biome, which is how a
 * cartographer's cabin ends up standing in the middle of a lake.
 * <p>
 * The wrapped structure lives in the registry on its own (as {@code <name>_inner}) but is in
 * no structure set, so it never generates by itself.
 */
public class DryLandStructure extends Structure {
    public static final Codec<DryLandStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Structure.CODEC.fieldOf("structure").forGetter(s -> s.inner)
    ).apply(i, DryLandStructure::new));

    /** Offsets sampled around the chunk centre: the footprint of a small building. */
    private static final int[][] SAMPLES = {{0, 0}, {-6, -6}, {6, 6}, {-6, 6}, {6, -6}};

    private final Holder<Structure> inner;

    public DryLandStructure(StructureSettings settings, Holder<Structure> inner) {
        super(settings);
        this.inner = inner;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        for (int[] o : SAMPLES) {
            int x = chunk.getMiddleBlockX() + o[0];
            int z = chunk.getMiddleBlockZ() + o[1];
            int top = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            NoiseColumn column = generator.getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            BlockState surface = column.getBlock(top);
            if (!surface.getFluidState().isEmpty()) return Optional.empty();
        }
        return inner.value().findValidGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.DRY_LAND.get();
    }
}
