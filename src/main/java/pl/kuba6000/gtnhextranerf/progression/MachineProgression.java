package pl.kuba6000.gtnhextranerf.progression;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.google.common.collect.Iterables;

import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.basic.MTEIceCreamMachine;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTESteamMultiBlockBase;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;

/** Server-authoritative processing gate. Basic-machine GUIs sync the missing certificate. */
public final class MachineProgression {

    private MachineProgression() {}

    public static ProgressionTier requiredTier(MetaTileEntity machine) {
        if (machine instanceof MTESteamMultiBlockBase) return ProgressionTier.STEAM;
        if (machine instanceof MTEMultiBlockBase) {
            MTEMultiBlockBase controller = (MTEMultiBlockBase) machine;
            int tier = 0;
            for (MTEHatch hatch : Iterables.concat(
                controller.mEnergyHatches,
                controller.mDynamoHatches,
                controller.getExoticEnergyHatches(),
                controller.getExoticDynamoHatches())) {
                if (EnergyHatchCertification.isEnergyHatch(hatch)) tier = Math.max(tier, hatch.mTier);
            }
            return ProgressionTier.forMachine(tier, false);
        }
        if (EnergyHatchCertification.isEnergyHatch(machine))
            return ProgressionTier.forMachine(((MTEHatch) machine).mTier, false);
        if (!(machine instanceof MTEBasicMachine)) return null;
        if (machine instanceof MTEIceCreamMachine) return null;
        if (isIndustrialApiary(machine)) return ProgressionTier.HV;
        return ProgressionTier.forMachine(((MTEBasicMachine) machine).mTier, machine.isSteampowered());
    }

    /**
     * GregTech registers the Industrial Apiary as a single UV machine, but it is meant to be usable from HV. Matched by
     * its registered ID, so Forestry's classes are never loaded.
     */
    private static boolean isIndustrialApiary(MetaTileEntity machine) {
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        return tile != null && tile.getMetaTileID() == MetaTileEntityIDs.INDUSTRIAL_APIARY.ID;
    }

    /** Checks the required tier against saved certificate masks without resolving the missing certificate's name. */
    public static boolean isLocked(MetaTileEntity machine) {
        ProgressionTier required = requiredTier(machine);
        if (required == null) return false;
        World world = worldOf(machine);
        if (world == null) return true;
        if (world.isRemote) return false;
        return data(world).isMissing(required);
    }

    public static ProgressionTier missingTier(MetaTileEntity machine) {
        ProgressionTier required = requiredTier(machine);
        if (required == null) return null;
        World world = worldOf(machine);
        if (world == null) return ProgressionTier.STEAM;
        if (world.isRemote) return null;
        return data(world).missing(required);
    }

    private static World worldOf(MetaTileEntity machine) {
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        return tile == null ? null : tile.getWorld();
    }

    public static boolean certify(World world, ProgressionTier tier) {
        ProgressionData data = data(world);
        boolean unlocked = data.unlock(tier);
        // Judged when the certificate is earned: waiting until a later report would let an old-enough world pass.
        if (unlocked && TelemetryEligibility.tooEarly(tier, world.getTotalWorldTime()) && data.blockTelemetry())
            GTNHExtraNerf.LOG.info(
                "Telemetry is off for this world for good: the {} certificate was earned before the world had run 5 hours",
                tier.id());
        return unlocked;
    }

    public static boolean isCertified(World world, ProgressionTier tier) {
        return data(world).isUnlocked(tier);
    }

    public static boolean revoke(World world, ProgressionTier tier) {
        return data(world).revoke(tier);
    }

    public static long certificateEpoch(World world, ProgressionTier tier) {
        return data(world).certificateEpoch(tier);
    }

    public static boolean isCurrentCertificate(World world, ProgressionTier tier, long issuedEpoch) {
        return data(world).isCurrentCertificate(tier, issuedEpoch);
    }

    private static ProgressionData data(World world) {
        if (world == null || world.isRemote) throw new IllegalStateException("Certification requires a server world");
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) throw new IllegalStateException("Progression overworld unavailable");
        ProgressionData data = (ProgressionData) overworld.mapStorage
            .loadData(ProgressionData.class, ProgressionData.NAME);
        if (data == null) {
            data = new ProgressionData();
            overworld.mapStorage.setData(ProgressionData.NAME, data);
        }
        return data;
    }
}
