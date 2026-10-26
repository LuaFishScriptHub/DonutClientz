package dev.basefinder;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** /bf radius <64-5000> | light | column | cards | loot | minpercent <n> | ratio <0.1-1> | maxspend <$> | prices | status */
public final class Commands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((d, reg) -> d.register(literal("bf")
            .then(literal("radius").then(argument("blocks", IntegerArgumentType.integer(64, 5000)).executes(c -> {
                Config.I.radius = IntegerArgumentType.getInteger(c, "blocks"); Config.save();
                c.getSource().sendFeedback(Component.literal("Radius = " + Config.I.radius)); return 1; })))
            .then(literal("minpercent").then(argument("p", IntegerArgumentType.integer(0, 99)).executes(c -> {
                Config.I.minPercent = IntegerArgumentType.getInteger(c, "p"); Config.save();
                c.getSource().sendFeedback(Component.literal("Min % = " + Config.I.minPercent)); return 1; })))
            .then(literal("ratio").then(argument("r", DoubleArgumentType.doubleArg(0.05, 1.0)).executes(c -> {
                Config.I.snipeRatio = DoubleArgumentType.getDouble(c, "r"); Config.save();
                c.getSource().sendFeedback(Component.literal("Snipe ratio = " + Config.I.snipeRatio)); return 1; })))
            .then(literal("maxspend").then(argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                Config.I.maxSpend = LongArgumentType.getLong(c, "amount"); Config.save();
                c.getSource().sendFeedback(Component.literal("Max spend = " + PriceBook.money(Config.I.maxSpend))); return 1; })))
            .then(literal("light").executes(c -> { Config.I.artificialLight = !Config.I.artificialLight; Config.save();
                c.getSource().sendFeedback(Component.literal("Artificial light: " + Config.I.artificialLight)); return 1; }))
            .then(literal("column").executes(c -> { Config.I.showColumn = !Config.I.showColumn; Config.save();
                c.getSource().sendFeedback(Component.literal("Column: " + Config.I.showColumn)); return 1; }))
            .then(literal("cards").executes(c -> { Config.I.showCards = !Config.I.showCards; Config.save();
                c.getSource().sendFeedback(Component.literal("Cards: " + Config.I.showCards)); return 1; }))
            .then(literal("loot").executes(c -> { Config.I.showLoot = !Config.I.showLoot; Config.save();
                c.getSource().sendFeedback(Component.literal("Loot panel: " + Config.I.showLoot)); return 1; }))
            .then(literal("prices").executes(c -> { PriceBook.refresh(m -> c.getSource().sendFeedback(Component.literal(m))); return 1; }))
            .then(literal("status").executes(c -> {
                c.getSource().sendFeedback(Component.literal("Bases: " + BaseManager.bases().size() + " | radius " + Config.I.radius
                    + " | prices " + PriceBook.size() + " | light " + Config.I.artificialLight)); return 1; })))
        );
    }
}
