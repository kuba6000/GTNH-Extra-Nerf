package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import kekztech.common.tileentities.MTELapotronicSuperCapacitor;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;

@Mixin(value = MTELapotronicSuperCapacitor.class, remap = false)
public interface MixinLapotronicLaserHatchAccessor {

    @Accessor("mEnergyTunnelsTT")
    Set<MTEHatchEnergyTunnel> gtnhextranerf$getLaserTargets();

    @Accessor("mDynamoTunnelsTT")
    Set<MTEHatchDynamoTunnel> gtnhextranerf$getLaserSources();
}
