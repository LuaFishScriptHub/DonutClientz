package dev.basefinder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit prices (per single item). Seeded from donut.auction, editable in
 * config/basefinder/prices.json. donut.auction has no documented public API, so
 * automatic refresh only works if you point "priceUrl" in config.json at a JSON file.
 */
public final class PriceBook {
    private static final Map<String, Double> PRICES = new HashMap<>();
    private static final Gson GSON = new Gson();

    static {
        seed("minecraft:elytra", 368_724_428);
        seed("minecraft:dragon_head", 56_473_939);
        seed("minecraft:piglin_head", 778_327);
        seed("minecraft:enchanted_golden_apple", 1_600_314);
        seed("minecraft:totem_of_undying", 61_981);
        seed("minecraft:golden_apple", 31_645);
        seed("minecraft:end_crystal", 27_040);
        seed("minecraft:firework_rocket", 558.95);
        seed("minecraft:netherite_ingot", 4_706_928);
        seed("minecraft:netherite_block", 41_800_208);
        seed("minecraft:ancient_debris", 1_409_756);
        seed("minecraft:netherite_sword", 6_279_304);
        seed("minecraft:netherite_helmet", 5_401_267);
        seed("minecraft:netherite_chestplate", 4_902_147);
        seed("minecraft:netherite_leggings", 5_283_631);
        seed("minecraft:netherite_boots", 5_026_953);
        seed("minecraft:redstone", 668.05);
        seed("minecraft:redstone_block", 2_100);
        seed("minecraft:repeater", 1_765);
        seed("minecraft:comparator", 3_303);
        seed("minecraft:hopper", 2_968);
        seed("minecraft:observer", 3_040);
        seed("minecraft:slime_block", 474.82);
        seed("minecraft:piston", 3_899);
        seed("minecraft:sticky_piston", 6_151);
    }

    private static void seed(String id, double v) { PRICES.put(id, v); }

    public static double unit(String id) { return PRICES.getOrDefault(id, 0.0); }
    public static int size() { return PRICES.size(); }

    public static void load() {
        try {
            Path p = Config.dir().resolve("prices.json");
            if (Files.exists(p)) {
                Map<String, Double> m = GSON.fromJson(Files.readString(p), new TypeToken<Map<String, Double>>() {}.getType());
                if (m != null) PRICES.putAll(m);
            } else {
                save();
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(Config.dir());
            Files.writeString(Config.dir().resolve("prices.json"), GSON.toJson(PRICES));
        } catch (Exception ignored) {}
    }

    /** Async refresh from Config.priceUrl. Returns a status message via callback. */
    public static void refresh(java.util.function.Consumer<String> done) {
        String url = Config.I.priceUrl;
        if (url == null || url.isBlank()) { done.accept("Set priceUrl in config.json first (donut.auction has no public API)."); return; }
        HttpClient.newHttpClient()
            .sendAsync(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString())
            .thenAccept(r -> {
                try {
                    Map<String, Double> m = GSON.fromJson(r.body(), new TypeToken<Map<String, Double>>() {}.getType());
                    PRICES.putAll(m); save();
                    done.accept("Loaded " + m.size() + " prices.");
                } catch (Exception e) { done.accept("Bad price JSON: " + e.getMessage()); }
            })
            .exceptionally(e -> { done.accept("Price refresh failed: " + e.getMessage()); return null; });
    }

    public static String money(double v) {
        if (v >= 1e9) return String.format("$%.1fB", v / 1e9);
        if (v >= 1e6) return String.format("$%.1fM", v / 1e6);
        if (v >= 1e3) return String.format("$%.1fK", v / 1e3);
        return String.format("$%.0f", v);
    }
}
