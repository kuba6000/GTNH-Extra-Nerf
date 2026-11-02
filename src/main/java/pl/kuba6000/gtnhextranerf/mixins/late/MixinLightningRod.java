package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.block.Block;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import gregtech.api.casing.Casings;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.generators.MTELightningRod;
import pl.kuba6000.gtnhextranerf.machines.ProgressionMachineNerf;

@Mixin(value = MTELightningRod.class, remap = false)
public abstract class MixinLightningRod implements ProgressionMachineNerf.LightningRodHook {

    @ModifyConstant(method = "maxEUStore", constant = @Constant(longValue = 50_000_000L), require = 1, allow = 1)
    private long gtnhextranerf$buffer(long original) {
        return ProgressionMachineNerf.LIGHTNING_ROD_BUFFER;
    }

    /**
     * @author GTNH Extra Nerf
     * @reason Replace GT 5.09.54.226's height-based strike chance and rain fallback, retaining native loss, cadence,
     *         lightning effects and fence consumption while checking the full world height for mast/sky validation.
     */
    @Overwrite
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        World world = tile.getWorld();
        if (world.isRemote) return;

        long energy = tile.getStoredEU();
        tile.setActive(energy > 0);
        if (energy > 0) tile.decreaseStoredEnergyUnits(energy / 100 + 1, false);
        if (tick % 256 != 0 || !world.isThundering()) return;

        int x = tile.getXCoord(), y = tile.getYCoord(), z = tile.getZCoord();
        int length = 0;
        boolean ended = false;
        for (int height = y + 1; height < world.getHeight(); height++) {
            Block block = tile.getBlock(x, height, z);
            if (!ended && block == Casings.IronFence.getBlock()) length++;
            else {
                ended = true;
                if (block != Blocks.air) return;
            }
        }
        if (length < 16 || world.rand.nextInt(128) >= Math.min(length, 32)) return;

        tile.increaseStoredEnergyUnits(tile.getEUCapacity() - tile.getStoredEU(), false);
        world.addWeatherEffect(new EntityLightningBolt(world, x, y + length, z));
        world.setBlockToAir(x, y + world.rand.nextInt(length) + 1, z);
    }
}
