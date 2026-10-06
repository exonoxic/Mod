package com.exonoxic.palimpsest.command;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.bleed.BleedCapability;
import com.exonoxic.palimpsest.bleed.BleedData;
import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.bleed.Ending;
import com.exonoxic.palimpsest.codex.CodexEntries;
import com.exonoxic.palimpsest.codex.CodexEntry;
import com.exonoxic.palimpsest.horror.EventContext;
import com.exonoxic.palimpsest.horror.HorrorDirector;
import com.exonoxic.palimpsest.horror.HorrorEvent;
import com.exonoxic.palimpsest.horror.HorrorEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/**
 * Operator tools for testing and for server owners who want to steer the experience:
 * <pre>
 * /palimpsest bleed get [player]
 * /palimpsest bleed set|add &lt;players&gt; &lt;amount&gt;
 * /palimpsest event &lt;id&gt; [player]      fire a Director event now (ignores cooldown/conditions)
 * /palimpsest codex unlockall [player]
 * /palimpsest reset &lt;players&gt;          clear Bleed, codex, flags and ending
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID)
public final class PalimpsestCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("palimpsest").requires(s -> s.hasPermission(2))
                .then(Commands.literal("bleed")
                        .then(Commands.literal("get")
                                .executes(c -> get(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> get(c, EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("set").then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0, 1000))
                                        .executes(c -> set(c, EntityArgument.getPlayers(c, "players"), FloatArgumentType.getFloat(c, "amount"))))))
                        .then(Commands.literal("add").then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(-1000, 1000))
                                        .executes(c -> add(c, EntityArgument.getPlayers(c, "players"), FloatArgumentType.getFloat(c, "amount")))))))
                .then(Commands.literal("event")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(HorrorEvents.ALL.stream().map(e -> e.id), b))
                                .executes(c -> fire(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> fire(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("codex").then(Commands.literal("unlockall")
                        .executes(c -> unlockAll(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> unlockAll(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("reset").then(Commands.argument("players", EntityArgument.players())
                        .executes(c -> reset(c, EntityArgument.getPlayers(c, "players"))))));
    }

    private static int get(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        BleedData data = BleedCapability.get(p);
        c.getSource().sendSuccess(() -> Component.literal(p.getScoreboardName() + ": Bleed " + String.format("%.1f", data.getBleed())
                + " (stage " + data.getStage().index + " " + data.getStage().name() + "), ending " + data.getEnding()
                + ", " + data.getCodex().size() + " codex entries, " + data.getApparitionsSeen() + " events"), false);
        return (int) data.getBleed();
    }

    private static int set(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer p : players) {
            BleedData data = BleedCapability.get(p);
            float delta = amount - data.getBleed();
            if (delta > 0) {
                // Route increases through add() so stage changes fire their messages and advancements;
                // divide out the gain multiplier so the result lands exactly on the requested value.
                float mult = BleedManager.gainMultiplier(p, data);
                if (mult > 0) BleedManager.add(p, delta / mult);
            }
            data.setBleed(amount);
            data.setNextEventTime(-1L);
            BleedManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.literal("Set Bleed to " + amount + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int add(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer p : players) BleedManager.add(p, amount);
        c.getSource().sendSuccess(() -> Component.literal("Added " + amount + " Bleed to " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int fire(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "id");
        HorrorEvent e = HorrorEvents.byId(id);
        if (e == null) {
            c.getSource().sendFailure(Component.literal("No such event: " + id));
            return 0;
        }
        boolean fired = HorrorDirector.fire(e, new EventContext(p, p.serverLevel(), BleedCapability.get(p)));
        c.getSource().sendSuccess(() -> Component.literal(fired ? "Fired " + id : "Event " + id + " found nothing suitable nearby"), true);
        return fired ? 1 : 0;
    }

    private static int unlockAll(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        for (CodexEntry e : CodexEntries.all()) BleedManager.unlock(p, e.id());
        c.getSource().sendSuccess(() -> Component.literal("Unlocked every Commonplace Book entry for " + p.getScoreboardName()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            BleedData fresh = new BleedData();
            fresh.setFirstSeen(p.level().getGameTime());
            BleedData data = BleedCapability.get(p);
            data.copyFrom(fresh);
            data.setEnding(Ending.NONE);
            p.refreshDisplayName();
            BleedManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.literal("Reset " + players.size() + " player(s)"), true);
        return players.size();
    }

    private PalimpsestCommand() {}
}
