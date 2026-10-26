package dev.basefinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saved to .minecraft/config/basefinder/config.json */
public final class Config {
    public boolean enabled = true;
    public boolean artificialLight = true;   // light sources raise the percentage
    public boolean showColumn = true;        // transparent box from base to surface
    public boolean showCards = true;
    public boolean showLoot = true;          // small loot panel next to the card
    public int radius = 5000;                // max distance of bases that are drawn
    public int minBuilt = 40;                // min weighted "built" score to count as a base
    public int minPercent = 25;              // hide anything below this
    public int maxCards = 4;
    public long lootThreshold = 1_000_000L;  // $ value of a stack to be listed
    public double snipeRatio = 0.70;         // buy if price <= value * ratio
    public long maxSpend = 5_000_000L;       // never buy anything above this
    public int snipeRefreshTicks = 30;
    public String priceUrl = "";             // optional JSON {"minecraft:elytra": 368000000, ...}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config I = new Config();

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("basefinder"); }

    public static void load() {
        try {
            Path p = dir().resolve("config.json");
            if (Files.exists(p)) I = GSON.fromJson(Files.readString(p), Config.class);
            if (I == null) I = new Config();
        } catch (Exception e) { I = new Config(); }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(dir());
            Files.writeString(dir().resolve("config.json"), GSON.toJson(I));
        } catch (Exception ignored) {}
    }
}
