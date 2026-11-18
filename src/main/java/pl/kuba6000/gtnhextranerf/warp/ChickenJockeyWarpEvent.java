package pl.kuba6000.gtnhextranerf.warp;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import shukaro.warptheory.handlers.IWarpEvent;
import shukaro.warptheory.handlers.WarpHandler;

public final class ChickenJockeyWarpEvent extends IWarpEvent {

    private static final int HORDE_SIZE = 24;
    private static final int INNER_RADIUS = 8;
    private static final int OUTER_RADIUS = 16;
    private static final int VERTICAL_SEARCH = 4;

    private ChickenJockeyWarpEvent() {
        super("gtnhextranerf.chicken_jockeys", 20);
    }

    public static void register() {
        WarpHandler.warpEvents.add(new ChickenJockeyWarpEvent());
    }

    @Override
    public boolean canDo(World world, EntityPlayer player) {
        return !world.isRemote && player.isEntityAlive()
            && !player.capabilities.disableDamage
            && world.difficultySetting != EnumDifficulty.PEACEFUL
            && world.getGameRules()
                .getGameRuleBooleanValue("doMobSpawning");
    }

    @Override
    public boolean doEvent(World world, EntityPlayer player) {
        if (!canDo(world, player)) return false;
        int centerX = MathHelper.floor_double(player.posX);
        int centerY = MathHelper.floor_double(player.boundingBox.minY);
        int centerZ = MathHelper.floor_double(player.posZ);
        int diameter = 2 * OUTER_RADIUS + 1;
        int spawned = 0;
        for (int attempt = 0; attempt < HORDE_SIZE * 6 && spawned < HORDE_SIZE; attempt++) {
            int x = centerX + world.rand.nextInt(diameter) - OUTER_RADIUS;
            int z = centerZ + world.rand.nextInt(diameter) - OUTER_RADIUS;
            double dx = x + 0.5 - player.posX;
            double dz = z + 0.5 - player.posZ;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < INNER_RADIUS * INNER_RADIUS || distanceSquared > OUTER_RADIUS * OUTER_RADIUS) {
                continue;
            }
            if (spawnInColumn(world, player, x, centerY, z)) spawned++;
        }
        if (spawned == 0) return false;
        sendChatMessage(player);
        return true;
    }

    private static boolean spawnInColumn(World world, EntityPlayer player, int x, int centerY, int z) {
        // Collision and liquid checks also inspect neighboring blocks
        if (!world.checkChunksExist(x - 2, 0, z - 2, x + 2, 0, z + 2)) return false;
        for (int step = 0; step <= 2 * VERTICAL_SEARCH; step++) {
            int offset = (step + 1) / 2;
            int y = centerY + (step % 2 == 0 ? -offset : offset);
            if (y <= 0 || y + 2 >= world.getActualHeight()
                || !World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                || !world.isAirBlock(x, y, z)
                || !world.isAirBlock(x, y + 1, z)) continue;
            return spawnPair(world, player, x + 0.5, y, z + 0.5);
        }
        return false;
    }

    private static boolean spawnPair(World world, EntityPlayer player, double x, int y, double z) {
        EntityChicken chicken = new EntityChicken(world);
        chicken.func_152117_i(true);
        chicken.setPosition(x, y, z);

        EntityZombie rider = new EntityZombie(world);
        rider.setChild(true);
        rider.setPosition(x, y + chicken.height * 0.5, z);

        AxisAlignedBB space = chicken.boundingBox.func_111270_a(rider.boundingBox.expand(0.1, 0, 0.1));
        if (!world.checkNoEntityCollision(space) || !world.getCollidingBoundingBoxes(rider, space)
            .isEmpty() || world.isAnyLiquid(space)) return false;

        if (!world.spawnEntityInWorld(chicken)) return false;
        if (!world.spawnEntityInWorld(rider)) {
            world.removeEntity(chicken);
            return false;
        }
        rider.mountEntity(chicken);
        chicken.updateRiderPosition();
        rider.setAttackTarget(player);
        return true;
    }
}
