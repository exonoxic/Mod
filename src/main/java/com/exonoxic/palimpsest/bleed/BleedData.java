package com.exonoxic.palimpsest.bleed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player state for the whole mod: Bleed value, discovered codex entries, one-shot flags,
 * the chosen ending, and the Director's pacing timers. Stored as a capability on the player.
 */
public class BleedData {
    private float bleed;
    private int highestStage;
    private Ending ending = Ending.NONE;
    private final Set<String> codex = new LinkedHashSet<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private long firstSeen = -1L;
    private long nextEventTime = -1L;
    private long lastRemedyTime = -100000L;
    private int apparitionsSeen;
    private boolean dirty = true;
    private boolean codexDirty = true;

    public float getBleed() {
        return bleed;
    }

    public void setBleed(float value) {
        float clamped = Math.max(0F, Math.min(BleedStage.MAX, value));
        if (clamped != bleed) {
            bleed = clamped;
            dirty = true;
        }
    }

    public BleedStage getStage() {
        return BleedStage.of(bleed);
    }

    public int getHighestStage() {
        return highestStage;
    }

    public void setHighestStage(int stage) {
        this.highestStage = stage;
    }

    public Ending getEnding() {
        return ending;
    }

    public void setEnding(Ending ending) {
        this.ending = ending;
        dirty = true;
    }

    public Set<String> getCodex() {
        return codex;
    }

    public boolean unlock(String entry) {
        if (codex.add(entry)) {
            codexDirty = true;
            return true;
        }
        return false;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    /** @return true if the flag was newly set. */
    public boolean setFlag(String flag) {
        return flags.add(flag);
    }

    public void clearFlag(String flag) {
        flags.remove(flag);
    }

    public boolean onCooldown(String key, long now) {
        Long until = cooldowns.get(key);
        return until != null && until > now;
    }

    public void setCooldown(String key, long until) {
        cooldowns.put(key, until);
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(long t) {
        firstSeen = t;
    }

    public long getNextEventTime() {
        return nextEventTime;
    }

    public void setNextEventTime(long t) {
        nextEventTime = t;
    }

    public long getLastRemedyTime() {
        return lastRemedyTime;
    }

    public void setLastRemedyTime(long t) {
        lastRemedyTime = t;
    }

    public int getApparitionsSeen() {
        return apparitionsSeen;
    }

    public void countApparition() {
        apparitionsSeen++;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isCodexDirty() {
        return codexDirty;
    }

    public void clean() {
        dirty = false;
        codexDirty = false;
    }

    public void markAllDirty() {
        dirty = true;
        codexDirty = true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Bleed", bleed);
        tag.putInt("HighestStage", highestStage);
        tag.putInt("Ending", ending.ordinal());
        tag.putLong("FirstSeen", firstSeen);
        tag.putLong("NextEvent", nextEventTime);
        tag.putLong("LastRemedy", lastRemedyTime);
        tag.putInt("Apparitions", apparitionsSeen);
        tag.put("Codex", writeSet(codex));
        tag.put("Flags", writeSet(flags));
        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putLong);
        tag.put("Cooldowns", cds);
        return tag;
    }

    public void load(CompoundTag tag) {
        bleed = tag.getFloat("Bleed");
        highestStage = tag.getInt("HighestStage");
        ending = Ending.byOrdinal(tag.getInt("Ending"));
        firstSeen = tag.contains("FirstSeen") ? tag.getLong("FirstSeen") : -1L;
        nextEventTime = tag.getLong("NextEvent");
        lastRemedyTime = tag.contains("LastRemedy") ? tag.getLong("LastRemedy") : -100000L;
        apparitionsSeen = tag.getInt("Apparitions");
        readSet(tag.getList("Codex", Tag.TAG_STRING), codex);
        readSet(tag.getList("Flags", Tag.TAG_STRING), flags);
        cooldowns.clear();
        CompoundTag cds = tag.getCompound("Cooldowns");
        for (String k : cds.getAllKeys()) cooldowns.put(k, cds.getLong(k));
        markAllDirty();
    }

    public void copyFrom(BleedData other) {
        load(other.save());
    }

    private static ListTag writeSet(Set<String> set) {
        ListTag list = new ListTag();
        for (String s : set) list.add(StringTag.valueOf(s));
        return list;
    }

    private static void readSet(ListTag list, Set<String> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) into.add(list.getString(i));
    }
}
