package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.world.WorldServer;

/** Loads construction chunks before native item placement in remote fixture areas. */
final class FixtureWorld {

    private FixtureWorld() {}

    static void loadAround(WorldServer world, int x, int z) {
        int chunkX = x >> 4, chunkZ = z >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) world.getChunkProvider()
                .loadChunk(chunkX + dx, chunkZ + dz);
        }
    }
}
