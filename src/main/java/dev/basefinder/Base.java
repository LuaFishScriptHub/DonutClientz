package dev.basefinder;

/** A detected base (cluster of nearby chunks with human-placed blocks). */
public final class Base {
    public double cx, cy, cz;
    public int minX, minY, minZ, maxX, maxY, maxZ;
    public int built, lights, lightBonus, percent, surfaceY, depth, columnTop;
    public boolean griefed, hasSurface;
}
