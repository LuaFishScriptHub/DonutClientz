package dev.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loop: close UI -> /ah -> read listings -> click anything priced <= value * ratio -> click confirm -> repeat.
 * Prices come from PriceBook (seeded from donut.auction). Only items present in the price book are considered.
 * TIP: sort the /ah by "lowest price" once yourself so the cheap ones are on page 1.
 */
public final class AuctionSniper {
    public static boolean armed;
    private enum S { START, WAIT_AH, WAIT_CONFIRM, COOLDOWN }
    private static S state = S.START;
    private static int timer, screenAge, lastContainerId = -1;
    private static final Pattern PRICE = Pattern.compile("\\$\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([KkMmBb]?)");

    public static void toggle(Minecraft mc) {
        armed = !armed;
        state = S.START; timer = 0;
        msg(mc, "Auction sniper " + (armed ? "ARMED (ratio " + Config.I.snipeRatio + ", max " + PriceBook.money(Config.I.maxSpend) + ")" : "OFF"));
    }

    public static void tick(Minecraft mc) {
        if (!armed || mc.player == null || mc.gameMode == null) return;
        if (timer > 0) timer--;
        switch (state) {
            case START -> {
                if (mc.screen instanceof AbstractContainerScreen<?>) mc.player.closeContainer();
                else if (mc.screen != null) mc.setScreen(null);
                mc.player.connection.sendCommand("ah");
                state = S.WAIT_AH; timer = 80; screenAge = 0;
            }
            case WAIT_AH -> {
                if (mc.screen instanceof AbstractContainerScreen<?> s) {
                    if (++screenAge < 6) return;                         // let the server fill the slots
                    Slot best = findDeal(s.getMenu(), mc);
                    if (best != null) {
                        lastContainerId = s.getMenu().containerId;
                        click(mc, s.getMenu(), best);
                        msg(mc, "Sniping: " + best.getItem().getHoverName().getString());
                        state = S.WAIT_CONFIRM; timer = 40; screenAge = 0;
                    } else if (timer <= 0 || screenAge > Config.I.snipeRefreshTicks) state = S.START;
                } else if (timer <= 0) state = S.START;
            }
            case WAIT_CONFIRM -> {
                if (mc.screen instanceof AbstractContainerScreen<?> s && ++screenAge > 4) {
                    Slot confirm = findConfirm(s.getMenu(), mc);
                    if (confirm != null) { click(mc, s.getMenu(), confirm); state = S.COOLDOWN; timer = 20; return; }
                }
                if (timer <= 0) state = S.START;
            }
            case COOLDOWN -> { if (timer <= 0) state = S.START; }
        }
    }

    private static void click(Minecraft mc, AbstractContainerMenu menu, Slot slot) {
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.PICKUP, mc.player);
    }

    private static Slot findDeal(AbstractContainerMenu menu, Minecraft mc) {
        Slot best = null;
        double bestRatio = Config.I.snipeRatio;
        for (Slot s : menu.slots) {
            if (s.container == mc.player.getInventory() || !s.hasItem()) continue;
            ItemStack st = s.getItem();
            double unit = PriceBook.unit(BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
            if (unit <= 0) continue;
            double price = listingPrice(st);
            if (price <= 0 || price > Config.I.maxSpend) continue;
            double ratio = price / (unit * st.getCount());
            if (ratio <= bestRatio) { bestRatio = ratio; best = s; }
        }
        return best;
    }

    private static Slot findConfirm(AbstractContainerMenu menu, Minecraft mc) {
        for (Slot s : menu.slots) {
            if (s.container == mc.player.getInventory() || !s.hasItem()) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem().getItem()).getPath();
            String name = s.getItem().getHoverName().getString().toLowerCase();
            if (name.contains("confirm") || id.equals("lime_stained_glass_pane") || id.equals("green_stained_glass_pane")
                || id.equals("lime_concrete") || id.equals("emerald_block")) return s;
        }
        return null;
    }

    /** Reads "$1,234" / "$12.5K" / "$3M" out of the item's lore. Returns 0 if not found. */
    static double listingPrice(ItemStack st) {
        ItemLore lore = st.get(DataComponents.LORE);
        if (lore == null) return 0;
        for (Component line : lore.lines()) {
            Matcher m = PRICE.matcher(line.getString());
            if (m.find()) {
                double v = Double.parseDouble(m.group(1).replace(",", ""));
                switch (m.group(2).toLowerCase()) {
                    case "k" -> v *= 1e3; case "m" -> v *= 1e6; case "b" -> v *= 1e9; default -> {}
                }
                return v;
            }
        }
        return 0;
    }

    private static void msg(Minecraft mc, String s) {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[BaseFinder] " + s), false);
    }
}
