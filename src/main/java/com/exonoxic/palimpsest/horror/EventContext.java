package com.exonoxic.palimpsest.horror;

import com.exonoxic.palimpsest.bleed.BleedData;
import com.exonoxic.palimpsest.bleed.BleedStage;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;

/** A snapshot of everything an event needs to decide whether it fits the moment. */
public final class EventContext {
    public final ServerPlayer player;
    public final ServerLevel level;
    public final BleedData data;
    public final BleedStage stage;
    public final boolean undertext;
    public final boolean overworld;
    public final boolean night;
    public final boolean indoors;
    public final boolean underground;
    public final boolean dark;
    public final double intensity;
    public final RandomSource random;
    public final long now;

    public EventContext(ServerPlayer player, ServerLevel level, BleedData data) {
        this.player = player;
        this.level = level;
        this.data = data;
        this.stage = data.getStage();
        this.undertext = level.dimension() == ModDimensions.UNDERTEXT;
        this.overworld = level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
        long dayTime = level.getDayTime() % 24000L;
        this.night = undertext || (dayTime > 12800L && dayTime < 23200L);
        BlockPos head = BlockPos.containing(player.getEyePosition());
        this.indoors = !level.canSeeSky(head);
        this.underground = indoors && player.getY() < level.getSeaLevel() - 6 && level.getBrightness(LightLayer.SKY, head) == 0;
        this.dark = level.getMaxLocalRawBrightness(head) <= 4;
        this.intensity = CommonConfig.HORROR_INTENSITY.get();
        this.random = player.getRandom();
        this.now = level.getGameTime();
    }

    public boolean atLeast(BleedStage s) {
        return stage.atLeast(s);
    }

    public boolean alterationsAllowed() {
        return CommonConfig.ALLOW_WORLD_ALTERATION.get();
    }
}
