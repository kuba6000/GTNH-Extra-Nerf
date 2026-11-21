package pl.kuba6000.gtnhextranerf.integration;

import java.math.BigInteger;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.misc.GTStructureChannels;
import kekztech.common.tileentities.MTELapotronicSuperCapacitor;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionData;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

@GameTestHolder("gtnhextranerftests")
public final class LapotronicCertificationScenarios {

    private LapotronicCertificationScenarios() {}

    @GameTest(template = "native_fixture")
    public static void laserHatchesConserveStoredEnergy(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        int x = 800;
        int y = 210;
        int z = 160;
        helper.afterTest(() -> {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    for (int dz = 0; dz < 5; dz++) world.setBlockToAir(x + dx, y + dy, z + dz);
                }
            }
        });
        ProgressionData data = (ProgressionData) world.mapStorage.loadData(ProgressionData.class, ProgressionData.NAME);
        if (data == null) {
            data = new ProgressionData();
            world.mapStorage.setData(ProgressionData.NAME, data);
        }
        NBTTagCompound saved = new NBTTagCompound();
        data.writeToNBT(saved);
        ProgressionData progression = data;
        helper.afterTest(() -> {
            progression.readFromNBT(saved);
            progression.markDirty();
        });
        for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);

        ItemStack stack = machine(MetaTileEntityIDs.lsc);
        BaseMetaTileEntity tile = TransformerIntegrationTests.placeAt(world, stack, x, y, z);
        tile.setFrontFacing(ForgeDirection.NORTH);
        MTELapotronicSuperCapacitor capacitor = (MTELapotronicSuperCapacitor) tile.getMetaTileEntity();
        ChannelDataAccessor.setChannelData(stack, GTStructureChannels.BOROGLASS.get(), 9);
        ChannelDataAccessor.setChannelData(stack, GTStructureChannels.LSC_CAPACITOR.get(), 2);
        capacitor.construct(stack, false);
        TransformerIntegrationTests.placeAt(world, machine(MetaTileEntityIDs.ZPM256AtLaserTargetHatch), x - 1, y, z);
        BaseMetaTileEntity source = TransformerIntegrationTests
            .placeAt(world, machine(MetaTileEntityIDs.IV256AtLaserSourceHatch), x + 1, y, z);
        TransformerIntegrationTests.placeAt(world, machine(MetaTileEntityIDs.AutoTapingMaintenanceHatch), x + 2, y, z);
        helper.assertTrue(capacitor.checkStructure(true, tile), "Lapotronic Supercapacitor did not form");

        capacitor.setStored(BigInteger.valueOf(100_000_000));
        tile.enableWorking();
        tick(tile, 130);
        helper.assertTrue(source.getStoredEU() > 0, "Certified capacitor did not supply its laser source");
        helper.assertTrue(source.setStoredEU(0), "Could not empty the laser source before revocation");
        BigInteger stored = capacitor.getStored();
        helper.assertTrue(stored.signum() > 0, "Fixture needs a nonempty capacitor after startup");

        MachineProgression.revoke(world, ProgressionTier.LV);
        tick(tile, 5);
        helper.assertTrue(
            capacitor.getStored()
                .equals(stored),
            "Revoked LV drained the capacitor through its laser source");
        helper.assertTrue(!tile.isActive(), "Capacitor remained active after LV revocation");
        MachineProgression.certify(world, ProgressionTier.LV);
        helper.assertTrue(source.getStoredEU() == 0, "Revoked LV changed the laser source buffer");

        MachineProgression.revoke(world, ProgressionTier.ZPM);
        tick(tile, 5);
        helper.assertTrue(
            capacitor.getStored()
                .equals(stored),
            "Uncertified ZPM laser target did not pause the capacitor");
        helper
            .assertTrue(source.getStoredEU() == 0, "Capacitor exported energy while its laser target was uncertified");
        MachineProgression.certify(world, ProgressionTier.ZPM);

        tick(tile, 1);
        long exported = source.getStoredEU();
        helper.assertTrue(exported > 0, "Recertified capacitor did not resume output");
        BigInteger expected = stored.subtract(BigInteger.valueOf(exported + capacitor.getPassiveDischargeAmount()));
        helper.assertTrue(
            capacitor.getStored()
                .equals(expected),
            "Recertified capacitor lost energy beyond native discharge");

        BaseMetaTileEntity uvSource = TransformerIntegrationTests
            .placeAt(world, machine(MetaTileEntityIDs.UV256AtLaserSourceHatch), x + 1, y, z);
        helper.assertTrue(capacitor.checkStructure(true, tile), "Capacitor did not form with its UV laser source");
        stored = capacitor.getStored();
        MachineProgression.revoke(world, ProgressionTier.UV);
        tick(tile, 5);
        helper.assertTrue(
            capacitor.getStored()
                .equals(stored),
            "Uncertified UV laser source drained the capacitor");
        MachineProgression.certify(world, ProgressionTier.UV);
        helper.assertTrue(uvSource.getStoredEU() == 0, "Uncertified UV laser source changed its buffer");
        helper.succeed();
    }

    private static ItemStack machine(MetaTileEntityIDs id) {
        return GregTechAPI.METATILEENTITIES[id.ID].getStackForm(1);
    }

    private static void tick(BaseMetaTileEntity tile, int count) {
        for (int tick = 0; tick < count; tick++) tile.updateEntityProfiled();
    }
}
