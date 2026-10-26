package dev.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * The client can only see what is inside a chest after you open it (this is a Minecraft limit,
 * not a mod limit). So loot is recorded whenever a container is opened, then shown on the base it belongs to.
 */
public final class LootLog {
    public record Entry(String name, int count, double value) {}
    private static final Map<BlockPos, List<Entry>> BY_POS = new HashMap<>();
    public static BlockPos last;
    public static long lastTick;

    public static void clear() { BY_POS.clear(); }

    public static void sample(Minecraft mc) {
        if (last == null || mc.player == null || mc.level == null) return;
        if (AuctionSniper.armed) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> s)) return;
        if (mc.level.getGameTime() - lastTick > 200) return;       // only right after we interacted with a block
        if (mc.level.getGameTime() % 10 != 0) return;
        List<Entry> list = new ArrayList<>();
        for (Slot slot : s.getMenu().slots) {
            if (slot.container == mc.player.getInventory() || !slot.hasItem()) continue;
            ItemStack st = slot.getItem();
            double v = PriceBook.unit(BuiltInRegistries.ITEM.getKey(st.getItem()).toString()) * st.getCount();
            if (v > 0) list.add(new Entry(st.getHoverName().getString(), st.getCount(), v));
        }
        BY_POS.put(last, list);
    }

    /** Valuable stacks (>= threshold) stored inside (or within 6 blocks of) a base. */
    public static List<Entry> valuable(Base b) {
        List<Entry> out = new ArrayList<>();
        for (var e : BY_POS.entrySet()) {
            BlockPos p = e.getKey();
            if (p.getX() < b.minX - 6 || p.getX() > b.maxX + 6 || p.getZ() < b.minZ - 6 || p.getZ() > b.maxZ + 6) continue;
            for (Entry en : e.getValue()) if (en.value() >= Config.I.lootThreshold) out.add(en);
        }
        out.sort((a, c) -> Double.compare(c.value(), a.value()));
        return out;
    }
}
