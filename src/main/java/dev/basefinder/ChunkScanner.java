package dev.basefinder;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class ChunkScanner {
    /** Returns null if the chunk has nothing interesting. Cheap: skips sections without relevant palette entries. */
    public static ChunkStat scan(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        ChunkStat st = new ChunkStat();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir()) continue;
            if (!sec.maybeHas(s -> BlockClassifier.relevant(s.getBlock()))) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState s = sec.getBlockState(x, y, z);
                        BlockClassifier.Info info = BlockClassifier.of(s.getBlock());
                        if (info != BlockClassifier.NONE) st.add(baseX + x, baseY + y, baseZ + z, info);
                    }
        }
        return (st.count > 0 || st.griefW > 0) ? st : null;
    }
}
