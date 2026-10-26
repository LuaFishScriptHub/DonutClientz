package dev.basefinder;

/** Aggregated result of scanning one chunk. */
public final class ChunkStat {
    public int count;           // human-placed block count
    public double weight;       // weighted score
    public int lights;
    public int griefW;
    public double sx, sy, sz;   // weight-weighted position sums (for centroid)
    public int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    public int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    public void add(int x, int y, int z, BlockClassifier.Info i) {
        griefW += i.griefW();
        if (i.weight() <= 0) return;
        count++;
        weight += i.weight();
        if (i.light()) lights++;
        sx += x * (double) i.weight(); sy += y * (double) i.weight(); sz += z * (double) i.weight();
        if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
        if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
    }
}
