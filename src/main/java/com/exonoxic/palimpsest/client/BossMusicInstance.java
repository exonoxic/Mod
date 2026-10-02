package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** The Rasure's theme: plays while it lives and fades away when it is gone. */
public class BossMusicInstance extends AbstractTickableSoundInstance {
    private final RasureEntity boss;
    private int fade = 40;

    public BossMusicInstance(RasureEntity boss) {
        super(ModSounds.MUSIC_RASURE.get(), SoundSource.RECORDS, RandomSource.create());
        this.boss = boss;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.9F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (boss.isRemoved() || !boss.isAlive()) {
            fade--;
            volume = Math.max(0F, 0.9F * fade / 40F);
            if (fade <= 0) stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
