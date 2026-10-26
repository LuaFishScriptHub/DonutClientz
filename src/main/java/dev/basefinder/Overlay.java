package dev.basefinder;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** All drawing: see-through column, base card and loot panel. Pure HUD drawing = no render-pipeline code. */
public final class Overlay {
    private static final int BG = 0xEB0B0F14, EDGE = 0xFF252D37, TXT = 0xFFE9EEF3, DIM = 0xFF8794A3,
        TRACK = 0xFF19202A, GOLD = 0xFFF2C14E;
    private static final int CARD_W = 112, CARD_H = 56, LOOT_W = 104;

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Freecam.frame(mc);
        Config c = Config.I;
        if (!c.enabled || mc.options.hideGui) return;

        Cam cam = Cam.capture(mc, g, dt.getGameTimeDeltaPartialTick(true));
        double px = cam.ex, pz = cam.ez;

        List<Base> list = new ArrayList<>();
        for (Base b : BaseManager.bases()) {
            double d = Math.hypot(b.cx - px, b.cz - pz);
            if (d <= c.radius && b.percent >= c.minPercent) list.add(b);
        }
        list.sort(Comparator.comparingDouble(b -> Math.hypot(b.cx - px, b.cz - pz)));
        if (list.size() > c.maxCards) list = list.subList(0, c.maxCards);

        for (Base b : list) {
            int accent = accent(b);
            if (c.showColumn) column(g, cam, b, accent);
            if (c.showCards) card(g, mc.font, cam, b, accent, c);
        }
    }

    static int accent(Base b) {
        if (b.griefed) return 0xFFE5484D;
        if (b.percent < 40) return 0xFF4DD4AC;
        if (b.percent < 70) return 0xFFF5B83D;
        return 0xFFFF7A45;
    }

    // ------------------------------------------------------------ column
    private static void column(GuiGraphics g, Cam cam, Base b, int accent) {
        double x0 = b.minX - 1, x1 = b.maxX + 2, z0 = b.minZ - 1, z1 = b.maxZ + 2;
        double y0 = b.minY - 1, y1 = b.columnTop;
        double[][] c = new double[8][];
        int i = 0;
        for (int yy = 0; yy < 2; yy++)
            for (int zz = 0; zz < 2; zz++)
                for (int xx = 0; xx < 2; xx++)
                    c[i++] = cam.cam(xx == 0 ? x0 : x1, yy == 0 ? y0 : y1, zz == 0 ? z0 : z1);
        int edge = (accent & 0x00FFFFFF) | 0xC8000000;
        int fill = (accent & 0x00FFFFFF) | 0x26000000;

        boolean allFront = true;
        for (double[] p : c) if (p[2] < 0.05) { allFront = false; break; }
        if (allFront) {
            List<double[]> pts = new ArrayList<>();
            for (double[] p : c) pts.add(cam.scr(p));
            cam.fillHull(g, pts, fill);
        }
        // 12 edges: index = y*4 + z*2 + x
        int[][] e = {{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
        for (int[] ed : e) cam.line(g, c[ed[0]], c[ed[1]], edge);
    }

    // ------------------------------------------------------------ card
    private static void card(GuiGraphics g, Font f, Cam cam, Base b, int accent, Config cfg) {
        double[] cc = cam.cam(b.cx, b.cy, b.cz);
        int sx, sy;
        if (cc[2] > 0.1) {
            double[] s = cam.scr(cc);
            sx = (int) s[0]; sy = (int) s[1];
        } else {                                   // behind you: pin to bottom edge on the correct side
            sx = cc[0] >= 0 ? cam.w - CARD_W : CARD_W;
            sy = cam.h - CARD_H;
        }
        int x = clamp(sx - CARD_W / 2, 4, cam.w - CARD_W - 4);
        int y = clamp(sy - CARD_H / 2, 4, cam.h - CARD_H - 4);

        double dx = b.cx - cam.ex, dz = b.cz - cam.ez;
        int dist = (int) Math.round(Math.hypot(dx, dz));

        rrect(g, x - 1, y - 1, CARD_W + 2, CARD_H + 2, EDGE);
        rrect(g, x, y, CARD_W, CARD_H, BG);
        g.fill(x + 1, y + 3, x + 3, y + CARD_H - 3, accent);             // accent strip

        int lx = x + 8, rx = x + CARD_W - 6;
        String title = b.griefed ? "Griefed base" : "Likely base";
        String pct = b.percent + "%";
        g.drawString(f, title, lx, y + 5, TXT);
        g.drawString(f, pct, rx - f.width(pct), y + 5, accent);

        g.drawString(f, dir(dx, dz) + " " + dist + "m", lx, y + 16, DIM);
        if (b.hasSurface) {
            String d = b.depth + " deep";
            g.drawString(f, d, rx - f.width(d), y + 16, DIM);
        }

        int bx0 = lx, bx1 = rx;
        g.fill(bx0, y + 27, bx1, y + 29, TRACK);
        g.fill(bx0, y + 27, bx0 + (bx1 - bx0) * b.percent / 100, y + 29, accent);

        String left = cfg.artificialLight ? "+" + b.lightBonus + " light" : "light off";
        String right = "built blocks " + b.built;
        g.drawString(f, left, lx, y + 34, accent);
        g.drawString(f, right, rx - f.width(right), y + 34, TXT);

        String xyz = "X " + (int) Math.round(b.cx) + "  Y " + (int) Math.round(b.cy) + "  Z " + (int) Math.round(b.cz);
        g.drawString(f, xyz, lx, y + 45, DIM);

        // loot panel (next to the card)
        if (cfg.showLoot) {
            List<LootLog.Entry> loot = LootLog.valuable(b);
            if (!loot.isEmpty()) lootPanel(g, f, loot, x + CARD_W + 4 + LOOT_W > cam.w ? x - LOOT_W - 4 : x + CARD_W + 4, y, cfg);
        }
    }

    private static void lootPanel(GuiGraphics g, Font f, List<LootLog.Entry> loot, int x, int y, Config cfg) {
        int rows = Math.min(4, loot.size());
        int h = 16 + rows * 10 + (loot.size() > rows ? 10 : 0);
        rrect(g, x - 1, y - 1, LOOT_W + 2, h + 2, EDGE);
        rrect(g, x, y, LOOT_W, h, BG);
        g.fill(x + 1, y + 3, x + 3, y + h - 3, GOLD);
        g.drawString(f, ">" + PriceBook.money(cfg.lootThreshold) + " loot", x + 8, y + 5, GOLD);
        int ty = y + 16;
        for (int i = 0; i < rows; i++) {
            LootLog.Entry e = loot.get(i);
            String v = PriceBook.money(e.value());
            String name = e.count() > 1 ? e.count() + "x " + e.name() : e.name();
            int max = LOOT_W - 16 - f.width(v);
            while (f.width(name) > max && name.length() > 3) name = name.substring(0, name.length() - 2);
            g.drawString(f, name, x + 8, ty, TXT);
            g.drawString(f, v, x + LOOT_W - 5 - f.width(v), ty, GOLD);
            ty += 10;
        }
        if (loot.size() > rows) g.drawString(f, "+" + (loot.size() - rows) + " more", x + 8, ty, DIM);
    }

    // ------------------------------------------------------------ helpers
    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x + 1, y, x + w - 1, y + h, col);
        g.fill(x, y + 1, x + w, y + h - 1, col);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static String dir(double dx, double dz) {
        String[] n = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        return n[Math.floorMod(Math.round(a / 45.0), 8)];
    }
}
