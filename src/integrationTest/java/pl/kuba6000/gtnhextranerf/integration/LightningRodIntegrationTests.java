package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.tileentities.generators.MTELightningRod;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;

/** Real server tiles, weather, fence geometry and generated EU through the public post-tick API. */
final class LightningRodIntegrationTests {

    private LightningRodIntegrationTests() {}

    static void verify(WorldServer world) {
        boolean rain = world.getWorldInfo()
            .isRaining(),
            thunder = world.getWorldInfo()
                .isThundering();
        String fire = world.getGameRules()
            .getGameRuleStringValue("doFireTick");
        int rainTime = world.getWorldInfo()
            .getRainTime(),
            thunderTime = world.getWorldInfo()
                .getThunderTime();
        world.getGameRules()
            .setOrCreateGameRule("doFireTick", "false");
        try {
            for (ItemList item : new ItemList[] { ItemList.Machine_HV_LightningRod, ItemList.Machine_EV_LightningRod,
                ItemList.Machine_IV_LightningRod }) {
                int high;
                try (Rod rod = new Rod(world, item, 128)) {
                    weather(world, true, true);
                    rod.mast(15);
                    rod.noGeneration(256, "Lightning Rod generated with a mast shorter than the minimum");
                    check(rod.machine.maxEUStore() < 50_000_000, "Lightning Rod retained its old energy buffer");
                    check(rod.machine.maxAmperesOut() == 512, "Lightning Rod amperage changed");
                    weather(world, true, false);
                    rod.mast(32);
                    rod.noGeneration(256, "Lightning Rod generated during ordinary rain");
                    weather(world, false, false);
                    rod.noGeneration(256, "Lightning Rod generated in clear weather");
                    weather(world, true, true);
                    world.setBlock(rod.tile.xCoord, 161, rod.tile.zCoord, Blocks.stone);
                    rod.noGeneration(256, "Lightning Rod ignored the blocked sky above its mast");
                    rod.mast(16);
                    world.setBlock(rod.tile.xCoord, world.getHeight() - 1, rod.tile.zCoord, Blocks.stone);
                    world.rand.setSeed(0x5A485552L);
                    rod.noGeneration(256, "Lightning Rod ignored an obstruction at the top of the world");
                    rod.mast(32);
                    world.setBlockToAir(rod.tile.xCoord, 136, rod.tile.zCoord);
                    rod.noGeneration(256, "Lightning Rod accepted a gap in its mast");
                    rod.mast(32);
                    for (int tick = 1; tick < 256; tick++) {
                        rod.machine.onPostTick(rod.tile, tick);
                        check(rod.tile.getStoredEU() == 0, "Lightning Rod attempted a strike between scheduled ticks");
                    }
                    high = rod.sample(16);
                    int longer = rod.sample(32);
                    check(high > 0 && longer > high && longer < 256, "Mast length did not provide a limited boost");
                    check(rod.sample(64) == longer, "Extending the mast beyond the cap increased its chance");
                    rod.tile.setStoredEU(rod.tile.getEUCapacity());
                    long before = rod.tile.getStoredEU();
                    rod.machine.onPostTick(rod.tile, 1);
                    check(
                        rod.tile.getStoredEU() < before && rod.tile.getStoredEU() > before * 9 / 10,
                        "Lightning Rod lost its native gradual buffer drain");
                }
                try (Rod rod = new Rod(world, item, 64)) {
                    check(rod.sample(16) == high, "Controller altitude changed the strike chance");
                }
                try (Rod rod = new Rod(world, item, world.getHeight() - 17)) {
                    check(rod.sample(16) > 0, "Lightning Rod ignored a mast segment at the top of the world");
                }
            }
        } finally {
            weather(world, rain, thunder);
            world.getWorldInfo()
                .setRainTime(rainTime);
            world.getWorldInfo()
                .setThunderTime(thunderTime);
            world.getGameRules()
                .setOrCreateGameRule("doFireTick", fire);
        }
        GTNHExtraNerf.LOG.info("Extra Nerf Lightning Rod integration tests PASSED");
    }

    private static void weather(WorldServer world, boolean rain, boolean thunder) {
        world.getWorldInfo()
            .setRaining(rain);
        world.getWorldInfo()
            .setThundering(thunder);
        world.getWorldInfo()
            .setRainTime(120_000);
        world.getWorldInfo()
            .setThunderTime(120_000);
        // Forge's public server weather hook avoids ticking unrelated chunk and machine state.
        for (int tick = 0; tick < 120; tick++) world.provider.updateWeather();
    }

    private static final class Rod implements AutoCloseable {

        final WorldServer world;
        final BaseMetaTileEntity tile;
        final MTELightningRod machine;

        Rod(WorldServer world, ItemList item, int y) {
            this.world = world;
            tile = TransformerIntegrationTests.placeAt(world, item.get(1), 1040, y, 240);
            tile.setFrontFacing(ForgeDirection.UP);
            machine = (MTELightningRod) tile.getMetaTileEntity();
        }

        void mast(int length) {
            for (int y = tile.yCoord + 1; y < world.getHeight(); y++) world.setBlockToAir(tile.xCoord, y, tile.zCoord);
            repair(length);
            tile.setStoredEU(0);
        }

        void repair(int length) {
            for (int i = 1; i <= length; i++)
                if (world.getBlock(tile.xCoord, tile.yCoord + i, tile.zCoord) != Casings.IronFence.getBlock())
                    world.setBlock(
                        tile.xCoord,
                        tile.yCoord + i,
                        tile.zCoord,
                        Casings.IronFence.getBlock(),
                        Casings.IronFence.getBlockMeta(),
                        2);
        }

        void noGeneration(int attempts, String message) {
            for (int attempt = 0; attempt < attempts; attempt++) {
                machine.onPostTick(tile, 256);
                check(tile.getStoredEU() == 0, message);
            }
        }

        int sample(int length) {
            mast(length);
            world.rand.setSeed(0x5A485552L);
            int hits = 0;
            for (int attempt = 0; attempt < 512; attempt++) {
                repair(length);
                tile.setStoredEU(0);
                machine.onPostTick(tile, 256);
                if (tile.getStoredEU() > 0) {
                    hits++;
                    check(tile.getStoredEU() == tile.getEUCapacity(), "A strike did not fill the reduced buffer");
                    int missing = 0;
                    for (int i = 1; i <= length; i++)
                        if (world.isAirBlock(tile.xCoord, tile.yCoord + i, tile.zCoord)) missing++;
                    check(missing == 1, "A strike did not consume exactly one mast block");
                }
                clearLightning();
            }
            return hits;
        }

        private void clearLightning() {
            world.weatherEffects.removeIf(
                entity -> entity instanceof EntityLightningBolt && entity.posX == tile.xCoord
                    && entity.posZ == tile.zCoord);
        }

        @Override
        public void close() {
            clearLightning();
            mast(0);
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
