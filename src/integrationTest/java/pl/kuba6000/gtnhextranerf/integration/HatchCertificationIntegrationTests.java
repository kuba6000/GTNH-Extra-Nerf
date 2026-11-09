package pl.kuba6000.gtnhextranerf.integration;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEWirelessEnergy;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/** Native energy APIs must respect certification without destroying a saved hatch buffer. */
final class HatchCertificationIntegrationTests {

    private HatchCertificationIntegrationTests() {}

    static void verify(WorldServer world) {
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Hatch_Energy_LV.get(1));
        try {
            for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
            check(tile.injectEnergyUnits(ForgeDirection.UNKNOWN, 32, 1) == 0, "Uncertified energy hatch accepted EU");
            check(!tile.isEnetInput(), "Uncertified energy hatch exposes its network port");
            MachineProgression.certify(world, ProgressionTier.STEAM);
            check(
                tile.injectEnergyUnits(ForgeDirection.UNKNOWN, 32, 1) == 0,
                "Steam bypassed electrical certification");
            MachineProgression.certify(world, ProgressionTier.LV);
            check(tile.injectEnergyUnits(ForgeDirection.UNKNOWN, 32, 1) == 1, "Certified energy hatch rejected EU");
            check(tile.getStoredEU() == 32, "Certified energy hatch did not store injected EU");
            MachineProgression.revoke(world, ProgressionTier.LV);
            check(tile.getStoredEU() == 0, "Revoked hatch exposes its preloaded buffer");
            check(!tile.decreaseStoredEnergyUnits(1, false), "Revoked hatch permits draining its buffer");
            check(!tile.increaseStoredEnergyUnits(1, false), "Revoked hatch permits filling its buffer");
            NBTTagCompound saved = new NBTTagCompound();
            tile.writeToNBT(saved);
            BaseMetaTileEntity restored = new BaseMetaTileEntity();
            restored.readFromNBT(saved);
            world.setTileEntity(tile.xCoord, tile.yCoord, tile.zCoord, restored);
            tile = restored;
            check(tile.getStoredEU() == 0, "NBT reload bypassed certification");
            MachineProgression.certify(world, ProgressionTier.LV);
            check(tile.getStoredEU() == 32, "Recertification did not restore the saved buffer");
            check(tile.decreaseStoredEnergyUnits(1, false), "Recertified energy hatch cannot supply energy");
            check(tile.getStoredEU() == 31, "Recertified hatch did not supply the requested EU");
        } finally {
            MachineProgression.revoke(world, ProgressionTier.LV);
            MachineProgression.revoke(world, ProgressionTier.STEAM);
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
        verifyWirelessReceivers(world);
        verifyRegisteredHatches(world);
        verifyNetworkOutput(world, false);
        verifyNetworkOutput(world, true);
        verifyWirelessOutput(world);
        verifyMaxGate(world);
        verifyControllerRequirements();
    }

    private static void verifyControllerRequirements() {
        MTEElectricBlastFurnace controller = new MTEElectricBlastFurnace("test.controller");
        check(MachineProgression.requiredTier(controller) == null, "Controller without energy hatches is gated");
        controller.mEnergyHatches.add(new MTEHatchEnergy("test.lv", 1, new String[0], null));
        controller.mEnergyHatches.add(new MTEHatchEnergy("test.hv", 3, new String[0], null));
        check(MachineProgression.requiredTier(controller) == ProgressionTier.HV, "Controller ignored its HV hatch");
        controller.mEnergyHatches.clear();
        controller.mDynamoHatches.add(new MTEHatchDynamo("test.mv", 2, new String[0], null));
        check(MachineProgression.requiredTier(controller) == ProgressionTier.MV, "Controller ignored its dynamo");
        controller.mDynamoHatches.clear();
        controller.getExoticEnergyHatches()
            .add(new MTEHatchEnergy("test.ev", 4, new String[0], null));
        check(MachineProgression.requiredTier(controller) == ProgressionTier.EV, "Controller ignored exotic input");
        controller.getExoticEnergyHatches()
            .clear();
        controller.getExoticDynamoHatches()
            .add(new MTEHatchDynamo("test.iv", 5, new String[0], null));
        check(MachineProgression.requiredTier(controller) == ProgressionTier.IV, "Controller ignored exotic output");
    }

    private static void verifyWirelessReceivers(WorldServer world) {
        for (Class<? extends MetaTileEntity> type : Arrays
            .asList(MTEWirelessEnergy.class, MTEHatchWirelessMulti.class)) {
            MetaTileEntity prototype = (MetaTileEntity) Arrays.stream(GregTechAPI.METATILEENTITIES)
                .filter(type::isInstance)
                .filter(hatch -> ((MTEHatch) hatch).mTier > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Native wireless receiver is missing: " + type));
            BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, prototype.getStackForm(1));
            for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
            UUID owner = tile.getOwnerUuid();
            BigInteger previous = WirelessNetworkManager.getUserEU(owner);
            BigInteger supply = BigInteger.ONE.shiftLeft(100);
            WirelessNetworkManager.setUserEU(owner, supply);
            try {
                fetch(tile);
                check(
                    WirelessNetworkManager.getUserEU(owner)
                        .equals(supply),
                    "Uncertified wireless receiver spent energy from the global account");
                check(tile.getStoredEU() == 0, "Uncertified wireless receiver filled its buffer");
                for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);
                fetch(tile);
                long stored = tile.getStoredEU();
                check(stored > 0, "Certified wireless receiver did not fetch energy");
                BigInteger balance = WirelessNetworkManager.getUserEU(owner);
                check(
                    balance.equals(supply.subtract(BigInteger.valueOf(stored))),
                    "Wireless receiver did not conserve energy between its account and buffer");
                MachineProgression.revoke(world, ProgressionTier.LV);
                fetch(tile);
                check(
                    WirelessNetworkManager.getUserEU(owner)
                        .equals(balance),
                    "Revoked wireless receiver spent more global energy");
                MachineProgression.certify(world, ProgressionTier.LV);
                check(tile.getStoredEU() == stored, "Revocation destroyed the wireless buffer");
            } finally {
                WirelessNetworkManager.setUserEU(owner, previous);
                for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.revoke(world, tier);
                world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
            }
        }
    }

    private static void fetch(BaseMetaTileEntity tile) {
        if (tile.getMetaTileEntity() instanceof MTEWirelessEnergy)
            ((MTEWirelessEnergy) tile.getMetaTileEntity()).tryFetchingEnergy();
        else((MTEHatchWirelessMulti) tile.getMetaTileEntity()).tryFetchingEnergy();
    }

    private static void verifyRegisteredHatches(WorldServer world) {
        int count = 0;
        certifyAll(world);
        try {
            for (var prototype : GregTechAPI.METATILEENTITIES) {
                if (!(prototype instanceof MTEHatchEnergy || prototype instanceof MTEHatchDynamo
                    || prototype instanceof MTEHatchEnergyMulti) || ((MTEHatch) prototype).mTier <= 0) continue;
                BaseMetaTileEntity tile = TransformerIntegrationTests
                    .place(world, ((MetaTileEntity) prototype).getStackForm(1));
                MetaTileEntity hatch = (MetaTileEntity) tile.getMetaTileEntity();
                for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
                tile.setStoredEU(64);
                long inputVoltage = tile.getInputVoltage(), outputVoltage = tile.getOutputVoltage();
                MachineProgression.revoke(world, ProgressionTier.LV);
                check(
                    tile.getStoredEU() == 0 && hatch.getEUVar() == 0,
                    "Registered hatch exposes locked energy: " + prototype.getLocalName());
                check(!tile.isEnetInput() && !tile.isEnetOutput(), "Registered hatch exposes a locked network port");
                for (ForgeDirection side : ForgeDirection.values()) {
                    check(
                        !tile.inputEnergyFrom(side) && !tile.outputsEnergyTo(side),
                        "Cached/sided port bypasses certification");
                    check(tile.injectEnergyUnits(side, 1, 1) == 0, "Registered hatch accepts locked energy");
                }
                check(
                    tile.getOfferedEnergy() == 0 && tile.demandedEnergyUnits() == 0 && tile.demandsEnergy() == 0,
                    "Legacy network API bypasses hatch certification");
                hatch.setEUVar(0);
                for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
                check(
                    tile.getInputVoltage() == inputVoltage && tile.getOutputVoltage() == outputVoltage,
                    "Certification changed native hatch voltage");
                MachineProgression.certify(world, ProgressionTier.LV);
                check(tile.getStoredEU() == 64, "Locked hatch lost or generated stored energy");
                world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
                count++;
            }
            check(count > 0, "No registered energy/dynamo hatches were exercised");
            GTNHExtraNerf.LOG.info("Extra Nerf certification verified {} registered hatch variants", count);
        } finally {
            revokeAll(world);
            world.setBlockToAir(800, 210, 160);
        }
    }

    private static void verifyNetworkOutput(WorldServer world, boolean laser) {
        MetaTileEntity source = laser ? (MetaTileEntity) Arrays.stream(GregTechAPI.METATILEENTITIES)
            .filter(MTEHatchDynamoTunnel.class::isInstance)
            .findFirst()
            .orElseThrow(AssertionError::new)
            : (MetaTileEntity) GregTechAPI.METATILEENTITIES[ItemList.Hatch_Dynamo_LV.get(1)
                .getItemDamage()];
        MetaTileEntity receiver = laser ? (MetaTileEntity) Arrays.stream(GregTechAPI.METATILEENTITIES)
            .filter(MTEHatchEnergyTunnel.class::isInstance)
            .filter(hatch -> ((MTEHatch) hatch).mTier == ((MTEHatch) source).mTier)
            .findFirst()
            .orElseThrow(AssertionError::new)
            : (MetaTileEntity) GregTechAPI.METATILEENTITIES[ItemList.Hatch_Energy_LV.get(1)
                .getItemDamage()];
        BaseMetaTileEntity dynamo = TransformerIntegrationTests.place(world, source.getStackForm(1));
        BaseMetaTileEntity energy = TransformerIntegrationTests.placeAt(world, receiver.getStackForm(1), 801, 210, 160);
        dynamo.setFrontFacing(ForgeDirection.EAST);
        energy.setFrontFacing(ForgeDirection.WEST);
        dynamo.setColorization((byte) 0);
        energy.setColorization((byte) 0);
        try {
            certifyAll(world);
            for (int tick = 0; tick < 25; tick++) {
                energy.updateEntityProfiled();
                dynamo.updateEntityProfiled();
            }
            long saved = dynamo.getEUCapacity();
            dynamo.setStoredEU(saved);
            energy.setStoredEU(0);
            MachineProgression.revoke(world, ProgressionTier.LV);
            for (int tick = 0; tick < 25; tick++) {
                energy.updateEntityProfiled();
                dynamo.updateEntityProfiled();
            }
            MachineProgression.certify(world, ProgressionTier.LV);
            check(dynamo.getStoredEU() == saved && energy.getStoredEU() == 0, "Unlicensed dynamo transferred energy");
            for (int tick = 0; tick < 25; tick++) {
                energy.updateEntityProfiled();
                dynamo.updateEntityProfiled();
            }
            check(
                dynamo.getStoredEU() < saved && energy.getStoredEU() > 0,
                "Recertified dynamo did not power its actual receiver (laser=" + laser + ")");
        } finally {
            revokeAll(world);
            world.setBlockToAir(800, 210, 160);
            world.setBlockToAir(801, 210, 160);
        }
    }

    private static void verifyWirelessOutput(WorldServer world) {
        MetaTileEntity prototype = (MetaTileEntity) Arrays.stream(GregTechAPI.METATILEENTITIES)
            .filter(MTEHatchWirelessDynamoMulti.class::isInstance)
            .findFirst()
            .orElseThrow(AssertionError::new);
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, prototype.getStackForm(1));
        MTEHatchWirelessDynamoMulti dynamo = (MTEHatchWirelessDynamoMulti) tile.getMetaTileEntity();
        for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
        UUID owner = tile.getOwnerUuid();
        BigInteger previous = WirelessNetworkManager.getUserEU(owner);
        try {
            certifyAll(world);
            tile.setStoredEU(64);
            MachineProgression.revoke(world, ProgressionTier.LV);
            dynamo.onPreTick(tile, WirelessNetworkManager.ticks_between_energy_addition);
            check(
                WirelessNetworkManager.getUserEU(owner)
                    .equals(previous),
                "Unlicensed wireless dynamo exported EU");
            MachineProgression.certify(world, ProgressionTier.LV);
            check(tile.getStoredEU() == 64, "Unlicensed wireless dynamo destroyed its buffer");
            dynamo.onPreTick(tile, WirelessNetworkManager.ticks_between_energy_addition);
            check(
                tile.getStoredEU() == 0 && WirelessNetworkManager.getUserEU(owner)
                    .equals(previous.add(BigInteger.valueOf(64))),
                "Recertified wireless dynamo did not conserve energy on export");
        } finally {
            WirelessNetworkManager.setUserEU(owner, previous);
            revokeAll(world);
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    private static void verifyMaxGate(WorldServer world) {
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Wireless_Hatch_Energy_MAX.get(1));
        try {
            certifyAll(world);
            MachineProgression.revoke(world, ProgressionTier.MAX);
            check(!tile.setStoredEU(64), "MAX hatch operates without its MAX certificate");
            MachineProgression.certify(world, ProgressionTier.MAX);
            check(tile.setStoredEU(64), "MAX certificate did not unlock the MAX hatch");
            MachineProgression.revoke(world, ProgressionTier.STEAM);
            check(!tile.decreaseStoredEnergyUnits(1, false), "MAX hatch bypassed its revoked Steam prerequisite");
            check(MachineProgression.isCertified(world, ProgressionTier.MAX), "Steam revocation erased MAX payment");
        } finally {
            revokeAll(world);
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    private static void certifyAll(WorldServer world) {
        for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);
    }

    private static void revokeAll(WorldServer world) {
        for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.revoke(world, tier);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
