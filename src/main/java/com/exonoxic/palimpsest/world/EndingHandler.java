package com.exonoxic.palimpsest.world;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.bleed.BleedCapability;
import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.bleed.Ending;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.network.VisionPacket;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.util.ServerScheduler;
import com.exonoxic.palimpsest.world.gate.GateTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The three things a player can do with the Last Folio at the Folio Stand.
 *
 * <ul>
 *   <li><b>Seal</b> — write your own name over the tear. Bleed goes to nothing and stays there;
 *   the Director stops; you are sent home with the Scrivener's Quill.</li>
 *   <li><b>Read</b> — read it aloud. The First Draft stays open around you for good (Bleed
 *   locked at the final stage), and you wear its circlet.</li>
 *   <li><b>Scrape</b> — only offered to someone carrying the Rasure Edge. Scrape your own name
 *   off the page. Inkborn no longer know you exist. Neither does chat.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID)
public final class EndingHandler {
    public static final int SEAL = 0;
    public static final int READ = 1;
    public static final int SCRAPE = 2;

    public static void choose(ServerPlayer player, int choice, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) return;
        if (!player.level().getBlockState(pos).is(ModBlocks.FOLIO_STAND.get())) return;
        if (BleedCapability.get(player).getEnding() != Ending.NONE) return;
        int slot = findFolio(player);
        if (slot < 0) return;
        if (choice == SCRAPE && !player.getInventory().contains(new ItemStack(ModItems.RASURE_EDGE.get()))) return;
        player.getInventory().removeItem(slot, 1);

        PacketHandler.sendTo(player, VisionPacket.of(VisionPacket.PAGE_TURN, 60, 1.0F));
        player.level().playSound(null, pos, ModSounds.RITUAL_COMPLETE.get(), SoundSource.PLAYERS, 1.5F, 0.6F);
        BleedManager.unlock(player, "the_choice");
        switch (choice) {
            case SEAL -> {
                BleedManager.setEnding(player, Ending.SEALED);
                give(player, new ItemStack(ModItems.SCRIVENERS_QUILL.get()));
                Advancements.grant(player, "ending_sealed");
                say(player, "message.palimpsest.ending.sealed");
                PacketHandler.sendTo(player, VisionPacket.of(VisionPacket.WHITEOUT, 80, 1.0F));
                ServerScheduler.schedule(70, () -> {
                    if (!player.hasDisconnected()) GateTravel.returnToOverworld(player, null);
                });
            }
            case READ -> {
                BleedManager.setEnding(player, Ending.READ);
                give(player, new ItemStack(ModItems.CIRCLET.get()));
                Advancements.grant(player, "ending_read");
                say(player, "message.palimpsest.ending.read");
                PacketHandler.sendTo(player, VisionPacket.of(VisionPacket.SKY_GLIMPSE, 200, 1.0F));
            }
            case SCRAPE -> {
                BleedManager.setEnding(player, Ending.UNWRITTEN);
                Advancements.grant(player, "ending_unwritten");
                say(player, "message.palimpsest.ending.unwritten");
                player.refreshDisplayName();
                player.refreshTabListName();
            }
            default -> {}
        }
    }

    private static int findFolio(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.LAST_FOLIO.get())) return i;
        }
        return -1;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static void say(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** The unwritten have no name. */
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (BleedCapability.get(event.getEntity()).getEnding() == Ending.UNWRITTEN) {
            event.setDisplayname(Component.literal("▒▒▒▒▒▒").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (BleedCapability.get(event.getEntity()).getEnding() == Ending.UNWRITTEN) {
            event.setDisplayName(Component.literal("▒▒▒▒▒▒").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private EndingHandler() {}
}
