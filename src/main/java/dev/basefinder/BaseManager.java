package dev.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public final class BaseManager {
    private static final Map<Long, ChunkStat> STATS = new HashMap<>();
    private static final ArrayDeque<LevelChunk> QUEUE = new ArrayDeque<>();
    private static List<Base> bases = List.of();
    private static boolean dirty;
    private static Level lastLevel;

    public static List<Base> bases() { return bases; }

    private static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    public static void reset() { STATS.clear(); QUEUE.clear(); bases = List.of(); dirty = false; }

    public static void onChunkLoad(LevelChunk c) {
        if (c.getLevel() != lastLevel) { reset(); lastLevel = c.getLevel(); }
        QUEUE.add(c);
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) return;
        if (mc.level != lastLevel) { reset(); lastLevel = mc.level; }
        for (int i = 0; i < 2 && !QUEUE.isEmpty(); i++) {
            LevelChunk c = QUEUE.poll();
            int cx = c.getPos().getMinBlockX() >> 4, cz = c.getPos().getMinBlockZ() >> 4;
            ChunkStat s = ChunkScanner.scan(c);
            if (s == null) { if (STATS.remove(key(cx, cz)) != null) dirty = true; }
            else { STATS.put(key(cx, cz), s); dirty = true; }
        }
        if (dirty && mc.level.getGameTime() % 20 == 0) { recluster(mc); dirty = false; }
    }

    // ---- clustering (union-find over chunks within 2 chunks of each other) ----
    private static long find(Map<Long, Long> p, long k) {
        while (p.get(k) != k) { p.put(k, p.get(p.get(k))); k = p.get(k); }
        return k;
    }

    private static void recluster(Minecraft mc) {
        Config cfg = Config.I;
        Map<Long, Long> parent = new HashMap<>();
        for (long k : STATS.keySet()) parent.put(k, k);
        for (long k : STATS.keySet()) {
            int cx = (int) (k >> 32), cz = (int) k;
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    long n = key(cx + dx, cz + dz);
                    if (parent.containsKey(n)) parent.put(find(parent, n), find(parent, k));
                }
        }
        Map<Long, List<ChunkStat>> groups = new HashMap<>();
        for (var e : STATS.entrySet()) groups.computeIfAbsent(find(parent, e.getKey()), x -> new ArrayList<>()).add(e.getValue());

        boolean ceiling = mc.level.dimensionType().hasCeiling();
        List<Base> out = new ArrayList<>();
        for (List<ChunkStat> g : groups.values()) {
            double w = 0, sx = 0, sy = 0, sz = 0;
            int count = 0, lights = 0, grief = 0;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (ChunkStat c : g) {
                w += c.weight; sx += c.sx; sy += c.sy; sz += c.sz;
                count += c.count; lights += c.lights; grief += c.griefW;
                if (c.count > 0) {
                    minX = Math.min(minX, c.minX); minY = Math.min(minY, c.minY); minZ = Math.min(minZ, c.minZ);
                    maxX = Math.max(maxX, c.maxX); maxY = Math.max(maxY, c.maxY); maxZ = Math.max(maxZ, c.maxZ);
                }
            }
            if (w < cfg.minBuilt || w <= 0) continue;

            Base b = new Base();
            b.cx = sx / w; b.cy = sy / w; b.cz = sz / w;
            b.built = count; b.lights = lights;
            b.griefed = grief >= 4;

            // Percentage depends mostly on how many blocks the base has.
            double pct = 100.0 * (1.0 - Math.exp(-w / 220.0));
            b.lightBonus = cfg.artificialLight ? (int) Math.min(15, lights * 1.5) : 0;
            pct += b.lightBonus;
            if (b.griefed) pct -= 20;
            b.percent = (int) Math.max(1, Math.min(99, Math.round(pct)));

            // keep the box a sensible size
            int cxI = (int) Math.round(b.cx), czI = (int) Math.round(b.cz);
            b.minX = Math.max(minX, cxI - 32); b.maxX = Math.min(maxX, cxI + 32);
            b.minZ = Math.max(minZ, czI - 32); b.maxZ = Math.min(maxZ, czI + 32);
            b.minY = minY; b.maxY = maxY;

            if (!ceiling) {
                b.hasSurface = true;
                b.surfaceY = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, cxI, czI);
                b.depth = Math.max(0, b.surfaceY - (int) Math.round(b.cy));
                b.columnTop = Math.max(b.surfaceY, b.maxY + 1);
            } else {
                b.columnTop = b.maxY + 6;
            }
            out.add(b);
        }
        bases = out;
    }
}
