package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.common.tileentities.machines.multi.steam.MTESteamWaterPump;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTESteamMultiBlockBase;

@Mixin(value = MTEExtendedPowerMultiBlockBase.class, remap = false)
public abstract class MixinSteamMultiBlockWailaData {

    @Inject(method = "getWailaNBTData", at = @At("RETURN"), require = 1, allow = 1)
    private void gtnhextranerf$syncSteamUsage(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world,
        int x, int y, int z, CallbackInfo callback) {
        Object self = this;
        if (!(self instanceof MTESteamMultiBlockBase) || self instanceof MTESteamWaterPump) return;

        MTESteamMultiBlockBase<?> machine = (MTESteamMultiBlockBase<?>) self;
        if (machine.getBaseMetaTileEntity() != null && machine.getBaseMetaTileEntity()
            .isActive() && machine.lEUt < 0) {
            // Match the running tick's integer division instead of GT's floating-point WAILA calculation.
            long amount = (-machine.lEUt * 10_000) / Math.max(1_000, machine.mEfficiency);
            tag.setLong("energyUsage", amount);
        }
    }
}
