package pl.kuba6000.gtnhextranerf.integration;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import shukaro.warptheory.handlers.IWarpEvent;
import shukaro.warptheory.handlers.WarpHandler;

@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class ChickenJockeyWarpTests {

    @GameTest(template = "native_fixture")
    public static void registeredWarpEventSpawnsMountedHorde(GameTestHelper helper) {
        NativeScenarios.scenario(helper, "registeredWarpEventSpawnsMountedHorde", false, () -> verifyHorde(helper));
    }

    private static void verifyHorde(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "JockeyWarpTest"));
        int center = 688;
        int floor = 210;
        FixtureWorld.loadAround(world, center, center);
        AxisAlignedBB area = AxisAlignedBB
            .getBoundingBox(center - 17, floor, center - 17, center + 18, floor + 10, center + 18);
        EnumDifficulty difficulty = world.difficultySetting;
        String spawning = world.getGameRules()
            .getGameRuleStringValue("doMobSpawning");
        helper.afterTest(() -> {
            world.difficultySetting = difficulty;
            world.getGameRules()
                .setOrCreateGameRule("doMobSpawning", spawning);
            entities(world, EntityZombie.class, area).forEach(Entity::setDead);
            entities(world, EntityChicken.class, area).forEach(Entity::setDead);
            for (int x = center - 16; x <= center + 16; x++) {
                for (int z = center - 16; z <= center + 16; z++) world.setBlockToAir(x, floor, z);
            }
        });
        for (int x = center - 16; x <= center + 16; x++) {
            for (int z = center - 16; z <= center + 16; z++) world.setBlock(x, floor, z, Blocks.stone, 0, 2);
        }
        player.setPosition(center + 0.5, floor + 1, center + 0.5);
        IWarpEvent raid = WarpHandler.getEventFromName("gtnhextranerf.chicken_jockeys");
        if (raid == null) throw new AssertionError("The horde is missing from the Warp Theory event pool");
        if (raid.getSeverity() != 20) throw new AssertionError("Expected the horde to require 20 effective warp");
        WarpHandler.queueEvent(player, raid);
        if (WarpHandler.dequeueEvent(player) != raid) throw new AssertionError("Warp Theory lost the queued effect");

        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "true");
        world.difficultySetting = EnumDifficulty.PEACEFUL;
        if (raid.canDo(world, player) || raid.doEvent(world, player)) {
            throw new AssertionError("The horde must not spawn on Peaceful");
        }
        world.difficultySetting = EnumDifficulty.NORMAL;
        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        if (raid.canDo(world, player) || raid.doEvent(world, player)) {
            throw new AssertionError("The horde must respect doMobSpawning");
        }
        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "true");
        if (!raid.doEvent(world, player)) throw new AssertionError("The horde failed to spawn on a clear floor");
        List<EntityZombie> riders = entities(world, EntityZombie.class, area);
        List<EntityChicken> mounts = entities(world, EntityChicken.class, area);
        if (riders.size() < 20 || riders.size() > 24 || riders.size() != mounts.size()) {
            throw new AssertionError("Expected a bounded horde of complete jockey pairs");
        }
        for (EntityZombie rider : riders) {
            double distance = Math.hypot(rider.posX - player.posX, rider.posZ - player.posZ);
            if (!rider.isChild() || !(rider.ridingEntity instanceof EntityChicken chicken)
                || !mounts.contains(chicken)
                || !chicken.func_152116_bZ()
                || chicken.riddenByEntity != rider
                || distance < 8
                || distance > 16) {
                throw new AssertionError("Expected a baby zombie on a jockey chicken 8 to 16 blocks away");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> List<T> entities(WorldServer world, Class<T> type, AxisAlignedBB area) {
        return world.getEntitiesWithinAABB(type, area);
    }
}
