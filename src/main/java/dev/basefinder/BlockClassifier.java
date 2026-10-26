package dev.basefinder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Decides which blocks are "placed by a human". Natural terrain is weight 0. */
public final class BlockClassifier {
    /** weight = how strongly it suggests a base, light = artificial light, griefW = obsidian/anchor evidence */
    public record Info(float weight, boolean light, int griefW) {}
    public static final Info NONE = new Info(0, false, 0);
    private static final Map<net.minecraft.world.level.block.Block, Info> CACHE = new IdentityHashMap<>();

    private static final Set<String> STRONG = Set.of(
        "chest", "trapped_chest", "ender_chest", "barrel", "furnace", "blast_furnace", "smoker",
        "crafting_table", "enchanting_table", "anvil", "chipped_anvil", "damaged_anvil", "brewing_stand",
        "beacon", "hopper", "dropper", "dispenser", "piston", "sticky_piston", "observer", "comparator",
        "repeater", "lectern", "smithing_table", "grindstone", "loom", "cartography_table", "stonecutter",
        "fletching_table", "jukebox", "note_block", "tnt", "conduit", "composter", "cauldron",
        "redstone_block", "slime_block", "honey_block", "iron_block", "gold_block", "diamond_block",
        "emerald_block", "netherite_block", "lapis_block", "copper_block", "ender_chest");

    public static boolean relevant(Block b) { return of(b) != NONE; }

    public static Info of(Block b) {
        Info i = CACHE.get(b);
        if (i == null) {
            i = compute(BuiltInRegistries.BLOCK.getKey(b).getPath());
            CACHE.put(b, i);
        }
        return i;
    }

    private static Info compute(String p) {
        if (p.equals("respawn_anchor")) return new Info(0, false, 4);
        if (p.equals("obsidian") || p.equals("crying_obsidian")) return new Info(0, false, 1);
        if (isLight(p)) return new Info(0.5f, true, 0);
        float w = weight(p);
        return w > 0 ? new Info(w, false, 0) : NONE;
    }

    private static boolean isLight(String p) {
        if (p.contains("torchflower")) return false;
        return p.equals("torch") || p.endsWith("_torch") || p.equals("wall_torch") || p.endsWith("_wall_torch")
            || p.endsWith("lantern") || p.equals("glowstone") || p.equals("redstone_lamp")
            || p.equals("end_rod") || p.endsWith("campfire") || p.equals("jack_o_lantern")
            || p.endsWith("_froglight");
    }

    private static float weight(String p) {
        if (STRONG.contains(p) || p.endsWith("shulker_box") || p.endsWith("_bed")) return 3f;
        if (p.contains("nether_brick")) return 0f;                  // natural fortresses
        if (p.contains("glass") && !p.contains("glazed")) return 2f;
        if (p.endsWith("concrete") || p.endsWith("_glazed_terracotta")) return 2f;
        if (p.endsWith("_wool") || p.endsWith("_door") || p.equals("bookshelf") || p.contains("quartz")) return 1.5f;
        if (p.endsWith("bricks") || p.endsWith("_fence_gate") || p.endsWith("_trapdoor") || p.endsWith("_sign")
            || p.endsWith("_banner") || p.equals("iron_bars") || p.equals("farmland")) return 1f;
        if (p.startsWith("polished_") || p.startsWith("smooth_") || p.startsWith("cut_") || p.startsWith("chiseled_")) return 0.8f;
        if (p.endsWith("_planks") || p.endsWith("_stairs") || p.endsWith("_slab") || p.endsWith("_carpet")
            || p.endsWith("_button") || p.endsWith("_pressure_plate") || p.equals("lever")) return 0.4f;
        if (p.endsWith("_fence") || p.equals("ladder") || p.equals("scaffolding") || p.equals("cobblestone")
            || p.equals("redstone_wire")) return 0.3f;
        return 0f;
    }
}
