package dev.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Own world->screen projection. This is how the ESP sees through walls without touching
 * the render pipeline (which changes every Minecraft version): everything is drawn on the HUD layer.
 */
public final class Cam {
    public double ex, ey, ez, rx, ry, rz, ux, uy, uz, fx, fy, fz, tanY, aspect;
    public int w, h;

    public static Cam capture(Minecraft mc, GuiGraphics g, float pt) {
        Entity e = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        Vec3 p = e.getEyePosition(pt);
        double yaw = Math.toRadians(e.getViewYRot(pt)), pitch = Math.toRadians(e.getViewXRot(pt));
        Cam c = new Cam();
        c.ex = p.x; c.ey = p.y; c.ez = p.z;
        c.fx = -Math.sin(yaw) * Math.cos(pitch); c.fy = -Math.sin(pitch); c.fz = Math.cos(yaw) * Math.cos(pitch);
        c.rx = -Math.cos(yaw); c.ry = 0; c.rz = -Math.sin(yaw);
        c.ux = c.ry * c.fz - c.rz * c.fy; c.uy = c.rz * c.fx - c.rx * c.fz; c.uz = c.rx * c.fy - c.ry * c.fx;
        c.w = g.guiWidth(); c.h = g.guiHeight();
        c.aspect = c.w / (double) c.h;
        c.tanY = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        return c;
    }

    /** camera-space coords {right, up, forward} */
    public double[] cam(double x, double y, double z) {
        double dx = x - ex, dy = y - ey, dz = z - ez;
        return new double[]{dx * rx + dy * ry + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
    }

    public double[] scr(double[] c) {
        return new double[]{
            w / 2.0 + (c[0] / c[2]) / (tanY * aspect) * (w / 2.0),
            h / 2.0 - (c[1] / c[2]) / tanY * (h / 2.0)};
    }

    public void line(GuiGraphics g, double[] a, double[] b, int col) {
        final double n = 0.05;
        if (a[2] < n && b[2] < n) return;
        if (a[2] < n) a = lerpNear(a, b, n); else if (b[2] < n) b = lerpNear(b, a, n);
        double[] s = scr(a), e = scr(b);
        double x0 = s[0], y0 = s[1], dx = e[0] - x0, dy = e[1] - y0;
        // Liang-Barsky clip to slightly larger than the screen
        double t0 = 0, t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 + 20, w + 20 - x0, y0 + 20, h + 20 - y0};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) { if (q[i] < 0) return; }
            else {
                double r = q[i] / p[i];
                if (p[i] < 0) { if (r > t1) return; if (r > t0) t0 = r; }
                else { if (r < t0) return; if (r < t1) t1 = r; }
            }
        }
        double ax = x0 + t0 * dx, ay = y0 + t0 * dy, bx = x0 + t1 * dx, by = y0 + t1 * dy;
        int steps = (int) Math.min(1200, Math.max(Math.abs(bx - ax), Math.abs(by - ay)));
        if (steps < 1) { g.fill((int) ax, (int) ay, (int) ax + 1, (int) ay + 1, col); return; }
        for (int i = 0; i <= steps; i++) {
            int x = (int) (ax + (bx - ax) * i / steps), y = (int) (ay + (by - ay) * i / steps);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    private static double[] lerpNear(double[] behind, double[] front, double n) {
        double t = (n - behind[2]) / (front[2] - behind[2]);
        return new double[]{behind[0] + (front[0] - behind[0]) * t, behind[1] + (front[1] - behind[1]) * t, n};
    }

    /** Fills the convex hull of the given screen points with a translucent colour. */
    public void fillHull(GuiGraphics g, java.util.List<double[]> pts, int col) {
        pts.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        java.util.List<double[]> hull = new java.util.ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int start = hull.size();
            for (int i = 0; i < pts.size(); i++) {
                double[] p = pts.get(pass == 0 ? i : pts.size() - 1 - i);
                while (hull.size() >= start + 2 && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0)
                    hull.remove(hull.size() - 1);
                hull.add(p);
            }
            hull.remove(hull.size() - 1);
        }
        if (hull.size() < 3) return;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] p : hull) { minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]); }
        for (int y = (int) Math.max(0, minY); y <= Math.min(h - 1, maxY); y++) {
            double xl = Double.MAX_VALUE, xr = -Double.MAX_VALUE;
            for (int i = 0; i < hull.size(); i++) {
                double[] p = hull.get(i), q = hull.get((i + 1) % hull.size());
                if ((p[1] <= y && q[1] > y) || (q[1] <= y && p[1] > y)) {
                    double x = p[0] + (y - p[1]) * (q[0] - p[0]) / (q[1] - p[1]);
                    xl = Math.min(xl, x); xr = Math.max(xr, x);
                }
            }
            if (xl <= xr) g.fill((int) Math.max(0, xl), y, (int) Math.min(w, xr), y + 1, col);
        }
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }
}
