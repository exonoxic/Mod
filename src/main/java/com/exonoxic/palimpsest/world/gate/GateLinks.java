package com.exonoxic.palimpsest.world.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Remembers which Overworld gate opens onto which Undertext gate. Stored with the Overworld. */
public class GateLinks extends SavedData {
    private static final String NAME = "palimpsest_gates";
    private final List<Link> links = new ArrayList<>();

    public record Link(BlockPos overworld, Direction overworldWidth, BlockPos undertext, Direction undertextWidth) {}

    public static GateLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(GateLinks::load, GateLinks::new, NAME);
    }

    @Nullable
    public Link byOverworld(BlockPos origin) {
        for (Link l : links) if (l.overworld().distManhattan(origin) <= 2) return l;
        return null;
    }

    @Nullable
    public Link byUndertext(BlockPos origin) {
        for (Link l : links) if (l.undertext().distManhattan(origin) <= 2) return l;
        return null;
    }

    public void put(Link link) {
        links.removeIf(l -> l.overworld().distManhattan(link.overworld()) <= 2 || l.undertext().distManhattan(link.undertext()) <= 2);
        links.add(link);
        setDirty();
    }

    public static GateLinks load(CompoundTag tag) {
        GateLinks data = new GateLinks();
        ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag l = list.getCompound(i);
            data.links.add(new Link(NbtUtils.readBlockPos(l.getCompound("Overworld")), Direction.from3DDataValue(l.getInt("OverworldWidth")),
                    NbtUtils.readBlockPos(l.getCompound("Undertext")), Direction.from3DDataValue(l.getInt("UndertextWidth"))));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Link link : links) {
            CompoundTag l = new CompoundTag();
            l.put("Overworld", NbtUtils.writeBlockPos(link.overworld()));
            l.putInt("OverworldWidth", link.overworldWidth().get3DDataValue());
            l.put("Undertext", NbtUtils.writeBlockPos(link.undertext()));
            l.putInt("UndertextWidth", link.undertextWidth().get3DDataValue());
            list.add(l);
        }
        tag.put("Links", list);
        return tag;
    }
}
