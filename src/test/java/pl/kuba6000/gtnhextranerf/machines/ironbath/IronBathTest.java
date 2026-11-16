package pl.kuba6000.gtnhextranerf.machines.ironbath;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import pl.kuba6000.gtnhextranerf.machines.IronBathStructure;

class IronBathTest {

    @Test
    void coldStartDoesNotLoseHeatBasedOnTheTemperatureReachedLaterInTheTick() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.IRON, 496 * 144));
        bath.tick(bath.heatDemand(bath.maximumTemperature()), Double.MAX_VALUE);
        assertEquals(bath.maximumTemperature(), bath.temperature(), 1e-9);
        assertEquals(0, bath.pendingMetalMb());
    }

    @Test
    void surfaceFollowsStoredLayersAndSurvivesFreezingWithoutIncludingQueuedMetal() {
        IronBath bath = heated(IronProduct.IRON, 128 * 144);
        assertEquals(new IronBath.Surface(0.25f, IronProduct.IRON, true), bath.surface());
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(
            IronProduct.STEEL,
            bath.surface()
                .product());
        assertEquals(1, bath.acceptCalcite(1));
        bath.tick(0, 0);
        assertEquals(32, bath.slagMb());
        IronBath.Surface liquid = bath.surface();
        assertEquals(IronProduct.SLAG, liquid.product());
        assertTrue(liquid.molten());
        assertEquals((128 * 144 + 32) / (float) bath.capacityMb(), liquid.fill());
        assertTrue(bath.addMetal(IronProduct.IRON, 64 * 144));
        assertEquals(liquid, bath.surface());
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setDouble("Heat", 0);
        bath.load(saved);
        assertEquals(new IronBath.Surface(liquid.fill(), IronProduct.SLAG, false), bath.surface());
        assertEquals(IronBath.Surface.EMPTY, new IronBath(0).surface());
    }

    @Test
    void surfaceBoundsCoverAllFiveTankRowsInEveryStructureAlignment() {
        var north = IronBathStructure.tankBounds(ExtendedFacing.DEFAULT, 0, 0, 0);
        assertEquals(7, north.minX);
        assertEquals(10, north.maxX);
        assertEquals(3, north.minY);
        assertEquals(8, north.maxY);
        assertEquals(0, north.minZ);
        assertEquals(3, north.maxZ);
        for (ExtendedFacing facing : ExtendedFacing.values()) {
            var bounds = IronBathStructure.tankBounds(facing, 300, 100, -40);
            double[] dimensions = { bounds.maxX - bounds.minX, bounds.maxY - bounds.minY, bounds.maxZ - bounds.minZ };
            Arrays.sort(dimensions);
            assertArrayEquals(new double[] { 3, 3, 5 }, dimensions, 1e-9);
        }
    }

    @Test
    void queuedMetalIsLimitedByTankVolumeInsteadOfHeatingPositions() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.IRON, 496 * 144));
        assertEquals(496 * 144, bath.pendingMetalMb());
        assertFalse(bath.addMetal(IronProduct.IRON, 144));
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(1, bath.chargeGroupCount());
        assertEquals(
            496,
            bath.chargeGroups(0, 1)
                .get(0)
                .count());
        bath.addHeat(496 * 32000, 496 * 32000);
        assertEquals(
            0.5,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(0, bath.metalMb());
    }

    @Test
    void carbonEntersTheBathImmediatelyWhileMetalIsQueued() {
        for (IronBath.Fuel fuel : IronBath.Fuel.values()) {
            IronBath bath = new IronBath(0);
            assertTrue(bath.addMetal(IronProduct.IRON, 16 * 144));
            double heatDemand = bath.chargeHeatDemand();
            double temperature = bath.temperature();
            assertTrue(bath.addFuel(fuel, 1));
            assertEquals(fuel.carbon(), bath.carbon());
            assertEquals(fuel.impurities(), bath.impurities());
            assertEquals(16, bath.meltingItems());
            assertEquals(1, bath.chargeGroupCount());
            assertEquals(heatDemand, bath.chargeHeatDemand());
            assertEquals(temperature, bath.temperature());
        }
    }

    @Test
    void savedFuelChargesBecomeResidentCarbonWithoutLosingPaidHeat() {
        IronBath bath = new IronBath(0);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        NBTTagCompound charge = new NBTTagCompound();
        charge.setInteger("Product", -1);
        charge.setInteger("Fuel", IronBath.Fuel.COAL.ordinal());
        charge.setInteger("Count", 2);
        charge.setDouble("Heat", 32000);
        saved.getCompoundTag("Charges")
            .setTag("0", charge);
        bath.load(saved);
        assertEquals(200, bath.carbon());
        assertEquals(10, bath.impurities());
        assertEquals(0, bath.meltingItems());
        assertEquals(0, bath.chargeGroupCount());
        NBTTagCompound restored = new NBTTagCompound();
        bath.save(restored);
        assertEquals(64000, restored.getDouble("Heat"));
    }

    @Test
    void directCarbonIntakeAcceptsOnlyThePartThatFits() {
        IronBath bath = new IronBath(0);
        assertEquals(102, bath.acceptFuel(IronBath.Fuel.COKE, 1000));
        assertEquals(10200, bath.carbon());
        assertEquals(102, bath.impurities());
        assertEquals(1, bath.acceptFuel(IronBath.Fuel.CACTUS_COKE, 64));
        assertEquals(0, bath.acceptFuel(IronBath.Fuel.CACTUS_COKE, 1));
        assertEquals(0, bath.meltingItems());
    }

    @Test
    void unrepresentableHeatingRemainderFinishesWithoutSpinning() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.IRON, 16 * 144));
        bath.addHeat(16 * 32000, 16 * 32000);
        double progress = bath.chargeGroups(0, 1)
            .get(0)
            .progress();
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> bath.addPower(1e-12, 1, 1));
        assertEquals(
            progress,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(16 * 144, bath.pendingMetalMb());
    }

    @Test
    void addingCarbonAtTheCeilingKeepsTemperatureAndConstituents() {
        IronBath bath = heated(IronProduct.IRON, 8 * 144);
        bath.addHeat(1_000_000, 0);
        assertEquals(2200, bath.temperature(), 1e-7);
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(2200, bath.temperature(), 1e-7);
        assertEquals(100, bath.carbon(), 1e-9);
        assertEquals(1, bath.impurities());
    }

    @Test
    void aSinglePaidAllowanceFinishesGroupsAtDifferentProgressWithoutDoubleSpending() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.IRON, 144));
        bath.addHeat(32000, 32000);
        assertTrue(bath.addMetal(IronProduct.IRON, 144));
        double before = bath.heatDemand(2200);
        bath.addPower(24000, 4, 1);
        assertEquals(0, bath.pendingMetalMb());
        assertEquals(2 * 144, bath.metalMb());
        assertEquals(96000, before - bath.heatDemand(2200), 1e-7);
    }

    @Test
    void automaticHeatRecoveryFollowsSuppliedPowerAndNotTheChargeConversion() {
        IronBath ordinary = new IronBath(2);
        IronBath stronger = new IronBath(2);
        ordinary.addPower(1000, 4, 1);
        stronger.addPower(2000, 4, 1);
        assertTrue(stronger.temperature() > ordinary.temperature());
        double demand = ordinary.powerDemand(4, 1);
        ordinary.addPower(demand, 4, 1);
        assertEquals(4000, ordinary.temperature(), 1e-7);
        assertEquals(0, ordinary.bathHeatDemand(), 1e-7);
    }

    @Test
    void hottestCoilsHaveSublinearNaturalCarbonBurn() {
        IronBath bath = heated(IronProduct.STEEL, 100 * 144);
        assertTrue(bath.configure(13));
        bath.addHeat(1_000_000_000, 0);
        assertEquals(13900, bath.temperature(), 1e-7);
        double before = bath.carbon();
        bath.cool(bath.heatLoss());
        bath.finishTick(true, true);
        double burned = before - bath.carbon();
        assertEquals(100 * 0.002 / 20 * 2 * Math.sqrt(13900.0 / 2200), burned, 1e-9);
        assertTrue(burned < 6 * 100 * 0.002 / 20);
    }

    @Test
    void paidPowerUsesChargeEfficiencyOnlyForTheChargeStage() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.IRON, 144));
        bath.addHeat(32000, 32000);
        double before = bath.heatDemand(2200);
        double requested = bath.powerDemand(4, 1);
        bath.addPower(10000, 4, 1);
        assertEquals(0, bath.pendingMetalMb());
        assertEquals(34000, before - bath.heatDemand(2200), 1e-7);
        assertEquals(requested - 10000, bath.powerDemand(4, 1), 1e-7);
    }

    @Test
    void coilsRaiseTheThermalCeilingWithoutChangingDirectBathHeating() {
        IronBath base = new IronBath(0);
        IronBath upgraded = new IronBath(13);
        assertEquals(2200, base.maximumTemperature());
        assertEquals(13900, upgraded.maximumTemperature());
        base.addPower(1000, 2, 1.5);
        upgraded.addPower(1000, 20, 1.5);
        assertEquals(base.temperature(), upgraded.temperature(), 1e-9);
        upgraded.addPower(10_000_000, 20, 1.5);
        assertEquals(13900, upgraded.temperature(), 1e-7);
        assertEquals(0, upgraded.powerDemand(20, 1.5), 1e-7);
    }

    @Test
    void identicalChargesKeepOneSharedProgressDuringHeatingCoolingAndReload() {
        IronBath bath = new IronBath(13);
        assertTrue(bath.addMetal(IronProduct.IRON, 700 * 144));
        bath.addHeat(700 * 32000, 700 * 32000);
        bath.cool(0);
        bath.finishTick(false, true);
        double progress = bath.chargeGroups(0, 1)
            .get(0)
            .progress();
        assertTrue(progress > 0 && progress < 0.5);
        assertEquals(1, bath.chargeGroupCount());
        assertEquals(
            700,
            bath.chargeGroups(0, 24)
                .get(0)
                .count());
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        IronBath loaded = new IronBath(0);
        loaded.load(saved);
        assertEquals(1, loaded.chargeGroupCount());
        assertEquals(
            progress,
            loaded.chargeGroups(0, 1)
                .get(0)
                .progress());
        loaded.addHeat(700 * 64000, 700 * 64000);
        assertEquals(700 * 144, loaded.metalMb());
        assertEquals(0, loaded.meltingItems());
    }

    @Test
    void groupsSeparateNbtAndProgressAndSpendHeatInProportionToTheirCounts() {
        IronBath bath = new IronBath(0);
        ItemStack plain = new ItemStack(new Item());
        ItemStack tagged = plain.copy();
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound()
            .setString("grade", "other");
        assertTrue(bath.addMetal(IronProduct.IRON, 2 * 144, plain));
        assertTrue(bath.addMetal(IronProduct.IRON, 3 * 144, plain));
        assertTrue(bath.addMetal(IronProduct.IRON, 144, tagged));
        assertEquals(2, bath.chargeGroupCount());
        bath.addHeat(6000, 6000);
        assertEquals(
            1000 / 64000.0,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(
            bath.chargeGroups(0, 1)
                .get(0)
                .progress(),
            bath.chargeGroups(1, 1)
                .get(0)
                .progress());
        assertTrue(bath.addMetal(IronProduct.IRON, 2 * 144, plain));
        assertEquals(3, bath.chargeGroupCount());
        assertEquals(
            0,
            bath.chargeGroups(2, 1)
                .get(0)
                .progress());
        bath.addHeat(8000, 8000);
        assertEquals(
            2000 / 64000.0,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(
            1000 / 64000.0,
            bath.chargeGroups(2, 1)
                .get(0)
                .progress());
        assertEquals(
            5,
            bath.chargeGroups(0, 24)
                .get(0)
                .count());
        assertTrue(
            ItemStack.areItemStacksEqual(
                plain,
                bath.chargeGroups(0, 24)
                    .get(0)
                    .stack()));
        assertEquals(
            1,
            bath.chargeGroups(1, 1)
                .size());
        assertTrue(
            bath.chargeGroups(20, 1)
                .isEmpty());
        ItemStack detached = bath.chargeGroups(1, 1)
            .get(0)
            .stack();
        detached.getTagCompound()
            .setString("grade", "changed");
        assertEquals(
            "other",
            bath.chargeGroups(1, 1)
                .get(0)
                .stack()
                .getTagCompound()
                .getString("grade"));
    }

    @Test
    void indexedSavedChargesMergeWithoutLosingPaidHeatOrMaterial() {
        NBTTagCompound saved = new NBTTagCompound();
        new IronBath(0).save(saved);
        NBTTagCompound entries = new NBTTagCompound();
        for (int index = 0; index < 4; index++) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("Product", IronProduct.STEEL.ordinal());
            entry.setInteger("Fuel", -1);
            entry.setDouble("Heat", index == 3 ? 0 : 32000);
            entries.setTag(Integer.toString(index), entry);
        }
        saved.setTag("Charges", entries);
        IronBath bath = new IronBath(0);
        bath.load(saved);
        assertEquals(2, bath.chargeGroupCount());
        assertEquals(
            3,
            bath.chargeGroups(0, 24)
                .get(0)
                .count());
        assertEquals(
            0.5,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(
            0,
            bath.chargeGroups(1, 1)
                .get(0)
                .progress());
        bath.save(saved);
        IronBath loaded = new IronBath(0);
        loaded.load(saved);
        assertEquals(bath.heatDemand(1800), loaded.heatDemand(1800));
        loaded.addHeat(4 * 64000, 4 * 64000);
        assertEquals(4 * 144, loaded.metalMb());
        assertEquals(4, loaded.carbon());
        assertEquals(0, loaded.meltingItems());
    }

    @Test
    void baseFurnaceReservesSixteenOfItsFiveHundredTwelveIngotsForSlag() {
        IronBath bath = new IronBath(0);
        assertEquals(512 * 144, bath.capacityMb());
        assertEquals(496 * 144, bath.metalCapacityMb());
    }

    @Test
    void partialMetalAdmissionLeavesExcessItemsOutsideWithoutBlockingCarbon() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.IRON, 480 * 144));
        assertEquals(16, bath.acceptMetal(IronProduct.STEEL, 64, null));
        assertEquals(496 * 144, bath.pendingMetalMb());
        assertEquals(0, bath.acceptMetal(IronProduct.IRON, 64, null));
        assertEquals(0, bath.acceptMetal(IronProduct.IRON, -1, null));
        assertEquals(0, bath.acceptMetal(IronProduct.SLAG, 1, null));
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(100, bath.carbon());
        assertEquals(1, bath.impurities());
        assertEquals(496, bath.meltingItems());
        assertEquals(2, bath.chargeGroupCount());
    }

    @Test
    void coilTierScalesVirtualCapacityWithoutChangingThePhysicalShell() {
        IronBath bath = new IronBath(0);
        bath.tick(10000, 10000);
        double temperature = bath.temperature();
        double demand = bath.heatDemand(1800);
        assertTrue(bath.configure(1));
        assertEquals(1024 * 144, bath.capacityMb());
        assertEquals(temperature, bath.temperature());
        assertEquals(demand, bath.heatDemand(1800));
        assertTrue(bath.configure(13));
        assertEquals(512 * 144 * 8192, bath.capacityMb());
        assertEquals(demand, bath.heatDemand(1800));
    }

    @Test
    void allQueuedItemsShareTheSuppliedHeatBudget() {
        for (int tier = 0; tier <= 1; tier++) {
            IronBath bath = new IronBath(tier);
            assertTrue(bath.addMetal(IronProduct.IRON, bath.metalCapacityMb()));
            double before = bath.heatDemand(2200);
            for (int tick = 0; tick < 300; tick++) bath.tick(1000, 2000);
            double transferred = before - bath.heatDemand(2200);
            assertTrue(transferred <= 300000 + 1e-6);
            assertTrue(transferred > 290000);
        }
    }

    @Test
    void largeQueuedGroupKeepsPaidHeatAcrossReloadAndAFittingCoilDowngrade() {
        IronBath bath = new IronBath(13);
        assertTrue(bath.addMetal(IronProduct.IRON, 699 * 144));
        double supplied = 349.09 * IronBath.COLD_MELTING_HEAT_PER_INGOT;
        bath.addHeat(supplied, supplied);
        assertEquals(0, bath.metalMb());
        assertEquals(699 * 144, bath.pendingMetalMb());
        double progress = bath.chargeGroups(0, 1)
            .get(0)
            .progress();
        assertTrue(progress < 0.5);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        IronBath loaded = new IronBath(0);
        loaded.load(saved);
        assertTrue(loaded.configure(1));
        assertEquals(699, loaded.meltingItems());
        assertEquals(
            progress,
            loaded.chargeGroups(0, 1)
                .get(0)
                .progress());
        loaded.addHeat(699 * IronBath.COLD_MELTING_HEAT_PER_INGOT, 699 * IronBath.COLD_MELTING_HEAT_PER_INGOT);
        assertEquals(0, loaded.meltingItems());
        assertEquals(699 * 144, loaded.metalMb());
    }

    @Test
    void repeatedHeatingRoundsAdvanceChemistryOnlyOnce() {
        IronBath bath = heated(IronProduct.STEEL, 100 * 144);
        double carbon = bath.carbon();
        for (int round = 0; round < 4; round++) {
            assertTrue(bath.addMetal(IronProduct.STEEL, 16 * 144));
            bath.addHeat(16 * IronBath.COLD_MELTING_HEAT_PER_INGOT, 16 * IronBath.COLD_MELTING_HEAT_PER_INGOT);
            assertEquals(0, bath.meltingItems());
        }
        assertEquals(164 * 144, bath.metalMb());
        assertEquals(carbon + 64, bath.carbon(), 1e-6);
        double before = bath.carbon();
        bath.cool(0);
        bath.finishTick(true, true);
        assertTrue(bath.carbon() < before);
        assertTrue(before - bath.carbon() < 164 * 0.002 / 20 * 2);
    }

    @Test
    void fullChargeUsesOnlyCoilHeatWithoutAcceleratingAtTheMeltingPoint() {
        IronBath bath = heated(IronProduct.IRON, 400 * 144);
        assertTrue(bath.addMetal(IronProduct.IRON, 16 * 144));
        bath.tick(0, 3200);
        assertEquals(
            0,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());

        double initialTemperature = bath.temperature();
        for (int tick = 1; tick < 640; tick++) {
            bath.tick(1600, 3200);
            assertEquals(
                tick / 640.0,
                bath.chargeGroups(0, 1)
                    .get(0)
                    .progress(),
                1e-9,
                "Constant paid heat must advance the whole group evenly");
            assertEquals(400 * 144, bath.liquidMetalMb());
        }
        bath.tick(1600, 3200);
        assertEquals(0, bath.meltingItems());
        assertEquals(416 * 144, bath.liquidMetalMb());
        assertTrue(bath.temperature() < initialTemperature);
        assertTrue(bath.temperature() > 2100);
    }

    @Test
    void hotBathStillRequestsCoilPowerForColdChargesAboveTheTargetTemperature() {
        IronBath bath = heated(IronProduct.IRON, 400 * 144);
        assertTrue(bath.addMetal(IronProduct.IRON, 16 * 144));
        for (int tick = 0; tick < 640; tick++) {
            double supplied = Math.min(1600, bath.heatDemand(1800));
            assertEquals(1600, supplied, 1e-6, "Hot resident metal cannot pay the queued charges' coil budget");
            bath.tick(supplied, 3200);
        }
        assertEquals(0, bath.meltingItems());
        assertEquals(416 * 144, bath.liquidMetalMb());
        assertEquals(0, bath.carbon());
    }

    @Test
    void calciteReserveHandlesAFullWorstFuelBatchAfterPartialConsumption() {
        for (int tier = 0; tier <= 1; tier++) {
            IronBath bath = new IronBath(tier);
            assertEquals(1, bath.acceptCalcite(1));
            assertTrue(bath.addFuel(IronBath.Fuel.COAL, 1));
            for (int tick = 0; tick < 100; tick++) bath.tick(10000, 20000);
            assertEquals(15, bath.calciteCapacity());
            int batch = 16 * (tier + 1);
            assertEquals(180 + 160 * tier, bath.calciteMaximum());
            assertEquals(batch / 2, bath.acceptCalcite(1000));
            assertEquals(batch * 10 + 15, bath.calciteCapacity());
            assertEquals(0, bath.acceptCalcite(1));
            assertTrue(bath.addFuel(IronBath.Fuel.CHARCOAL, batch));
            for (int tick = 0; tick < 400; tick++) bath.tick(10000, 20000);
            assertEquals(0, bath.impurities());
            assertEquals(15, bath.calciteCapacity());
            assertEquals((5 + batch * 10) * 32, bath.slagMb());
            assertEquals(0, bath.acceptCalcite(-1));
        }
    }

    @Test
    void configurationAndPersistencePreserveChargesAboveTheOldArraySize() {
        IronBath original = new IronBath(2);
        assertTrue(original.addMetal(IronProduct.IRON, 40 * 144));
        for (int tick = 0; tick < 9; tick++) original.tick(1000, 500);
        double progress = original.chargeGroups(0, 1)
            .get(0)
            .progress();
        assertTrue(progress > 0);
        double demand = original.heatDemand(1800);
        assertTrue(original.configure(1));
        assertEquals(demand, original.heatDemand(1800));
        NBTTagCompound tag = new NBTTagCompound();
        original.save(tag);
        IronBath restored = new IronBath(0);
        restored.load(tag);
        assertEquals(40 * 144, restored.pendingMetalMb());
        assertEquals(
            progress,
            restored.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertTrue(restored.configure(3));
        assertEquals(
            progress,
            restored.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(demand, restored.heatDemand(1800));
        assertTrue(original.configure(3));
        for (int tick = 0; tick < 400; tick++) {
            original.tick(10000, 20000);
            restored.tick(10000, 20000);
        }
        assertEquals(original.metalMb(), restored.metalMb());
        assertEquals(original.temperature(), restored.temperature(), 1e-8);
    }

    @Test
    void fittingDowngradeKeepsQueuedHeatAndAllConstituents() {
        IronBath bath = new IronBath(1);
        assertTrue(bath.addMetal(IronProduct.STEEL, 8 * 144));
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(1, bath.acceptCalcite(1));
        for (int tick = 0; tick < 9; tick++) bath.tick(1000, 500);
        double demand = bath.heatDemand(1800);
        double progress = bath.chargeGroups(0, 1)
            .get(0)
            .progress();
        assertTrue(bath.configure(0));
        assertEquals(8 * 144, bath.pendingMetalMb());
        assertEquals(
            progress,
            bath.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertEquals(100, bath.carbon());
        assertEquals(demand, bath.heatDemand(1800));
        assertEquals(20, bath.calciteCapacity());
    }

    @Test
    void savedChargesIgnoreObsoleteHeatingPositionLimits() {
        IronBath original = new IronBath(1);
        assertTrue(original.addMetal(IronProduct.IRON, 24 * 144));
        for (int tick = 0; tick < 9; tick++) original.tick(1000, 500);
        NBTTagCompound tag = new NBTTagCompound();
        original.save(tag);
        tag.removeTag("CoilTier");
        tag.setInteger("MeltingSlots", 1);
        tag.setInteger("Capacity", 25088 * 2);
        tag.setInteger("CrossSection", 25088);
        IronBath restored = new IronBath(0);
        restored.load(tag);
        assertEquals(24 * 144, restored.pendingMetalMb());
        assertEquals(
            original.chargeGroups(0, 1)
                .get(0)
                .progress(),
            restored.chargeGroups(0, 1)
                .get(0)
                .progress());
        assertTrue(restored.configure(0));
        assertEquals(original.heatDemand(1800), restored.heatDemand(1800));
    }

    @Test
    void downgradeRetainsSurplusCalciteButRejectsOversizedMetalContents() {
        IronBath bath = new IronBath(1);
        assertEquals(17, bath.acceptCalcite(17));
        assertTrue(bath.configure(0));
        assertEquals(340, bath.calciteCapacity());
        assertEquals(0, bath.acceptCalcite(1));
        NBTTagCompound tag = new NBTTagCompound();
        bath.save(tag);
        bath.load(tag);
        assertEquals(340, bath.calciteCapacity());
        assertEquals(180, bath.calciteMaximum());
        assertEquals(0, bath.acceptCalcite(1));
        tag.setInteger("Calcite", 20);
        tag.setInteger("Metal", 512 * 144 + 144);
        tag.setInteger("Slag", 320);
        tag.setDouble("Carbon", 42);
        bath.load(tag);
        assertFalse(bath.configure(0));
        assertEquals(512 * 144 + 144, bath.metalMb());
        assertEquals(320, bath.slagMb());
        assertEquals(42, bath.carbon());
        assertEquals(20, bath.calciteCapacity());
    }

    @Test
    void carbonAddsNoHeatOrHeatingDemand() {
        IronBath bath = new IronBath(0);
        double demand = bath.heatDemand(1800);
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(0, bath.meltingItems());
        assertEquals(100, bath.carbon());
        assertEquals(300, bath.temperature());
        assertEquals(demand, bath.heatDemand(1800));
    }

    @Test
    void coldChargeNeedsHeatingBeforeItCanBecomeLiquid() {
        IronBath bath = new IronBath(0);
        assertTrue(bath.addMetal(IronProduct.STEEL, 144));
        assertEquals(0, bath.liquidMetalMb());
        assertEquals(144, bath.pendingMetalMb());
        for (int tick = 0; tick < 100; tick++) bath.tick(10000, 10000);
        assertEquals(144, bath.metalMb());
        assertTrue(bath.liquidMetalMb() > 0);
    }

    @Test
    void tapPrioritizesSlagPresentInItsBand() {
        IronBath bath = heated(IronProduct.IRON, 69 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Metal", 10000);
        saved.setInteger("Slag", 1);
        bath.load(saved);
        bath.addHeat(1_000_000_000, 0);

        IronBath.Tap preview = bath.previewTap(0, 144);
        assertEquals(IronProduct.SLAG, preview.product());
        assertEquals(1, preview.amountMb());
        assertEquals(10000, bath.metalMb());
        assertEquals(1, bath.slagMb());
        IronBath.Tap drained = bath.tap(0, 144);
        assertEquals(IronProduct.SLAG, drained.product());
        assertEquals(1, drained.amountMb());
        assertEquals(
            IronProduct.IRON,
            bath.previewTap(0, 144)
                .product());
    }

    @Test
    void previewDoesNotConsumeAndTappingDebitsCanonicalConstituents() {
        IronBath bath = heated(IronProduct.STEEL, 20 * 144);
        double carbon = bath.carbon();
        IronBath.Tap preview = bath.previewTap(0, 144);
        assertEquals(IronProduct.STEEL, preview.product());
        assertEquals(144, preview.amountMb());
        assertEquals(carbon, bath.carbon());
        assertEquals(20 * 144, bath.metalMb());
        IronBath.Tap actual = bath.tap(0, 144);
        assertEquals(preview.product(), actual.product());
        assertEquals(preview.amountMb(), actual.amountMb());
        assertEquals(19 * 144, bath.metalMb());
        assertEquals(carbon - 1, bath.carbon(), 1e-9);
    }

    @Test
    void atomicTapKeepsItsStartingProductAcrossBandsAndRemeltsItsCanonicalConstituents() {
        IronBath bath = heated(IronProduct.STEEL, 8 * 144);
        assertTrue(bath.addMetal(IronProduct.CAST_IRON, 8 * 144));
        for (int tick = 0; tick < 100; tick++) bath.tick(10000, 20000);
        assertEquals(16 * 144, bath.liquidMetalMb());
        assertEquals(IronProduct.STEEL, bath.currentProduct());
        double carbon = bath.carbon();
        double temperature = bath.temperature();

        IronBath.Tap preview = bath.previewTap(0, 12 * 144);
        assertEquals(IronProduct.STEEL, preview.product());
        assertEquals(12 * 144, preview.amountMb());
        assertEquals(16 * 144, bath.metalMb());
        assertEquals(carbon, bath.carbon());

        IronBath.Tap tapped = bath.tap(0, 12 * 144);
        assertEquals(IronProduct.STEEL, tapped.product());
        assertEquals(12 * 144, tapped.amountMb());
        assertEquals(4 * 144, bath.metalMb());
        assertEquals(carbon - 12, bath.carbon(), 1e-9);
        assertEquals(IronProduct.PIG_IRON, bath.currentProduct());
        assertEquals(temperature, bath.temperature(), 1e-9);

        assertTrue(bath.addMetal(tapped.product(), tapped.amountMb()));
        bath.tick(1_000_000, 1_000_000);
        assertEquals(16 * 144, bath.metalMb());
        assertEquals(0, bath.pendingMetalMb());
        // At 2200 K, sixteen liquid ingots burn this much carbon during the remelting tick.
        double burned = 16 * 0.002 / 20 * 2;
        assertEquals(carbon - burned, bath.carbon(), 1e-9);
        assertEquals(IronProduct.STEEL, bath.currentProduct());
    }

    @Test
    void completedChargeJoinsTheLiquidBathAtItsMeltingTemperature() {
        IronBath bath = heated(IronProduct.IRON, 8 * 144);
        double before = bath.temperature();
        assertTrue(bath.addMetal(IronProduct.STEEL, 144));
        assertEquals(8 * 144, bath.metalMb());
        assertEquals(before, bath.temperature());
        bath.tick(64000, 64000);
        assertEquals(0, bath.pendingMetalMb());
        assertEquals(9 * 144, bath.liquidMetalMb());
        double previousCapacity = 128000.0 / 1400 + 8 * 20;
        double mixedTemperature = (before * previousCapacity + 1700 * 20) / (previousCapacity + 20);
        double standingLoss = 30 * 64000.0 / 24000 * (before - 300) / 1900;
        assertEquals(mixedTemperature - 2 * standingLoss / (previousCapacity + 20), bath.temperature(), 1e-9);
    }

    @Test
    void continuousIronFeedDoesNotStarveQueuedPigIron() {
        IronBath bath = heated(IronProduct.IRON, 8 * 144);
        assertTrue(bath.addMetal(IronProduct.IRON, 15 * 144));
        assertTrue(bath.addMetal(IronProduct.PIG_IRON, 144));
        int withdrawn = 0;
        boolean receivedPigIronCarbon = false;
        for (int tick = 0; tick < 100000; tick++) {
            bath.tick(60, 120);
            if (bath.carbon() > 0) {
                receivedPigIronCarbon = true;
                break;
            }
            withdrawn += bath.tap(0, 144)
                .amountMb();
            while (withdrawn >= 144) {
                assertTrue(bath.addMetal(IronProduct.IRON, 144));
                withdrawn -= 144;
            }
        }
        assertTrue(receivedPigIronCarbon, "Queued Pig Iron must join a continuously replenished Iron bath");
    }

    @Test
    void drainingExtremeProductsMovesTheRemainderFurtherFromSteel() {
        IronBath low = heated(IronProduct.WROUGHT_IRON, 20 * 144);
        double lowRatio = low.carbon() / low.metalMb();
        assertEquals(
            IronProduct.WROUGHT_IRON,
            low.tap(0, 144)
                .product());
        assertTrue(low.carbon() / low.metalMb() < lowRatio);

        IronBath high = heated(IronProduct.PIG_IRON, 20 * 144);
        assertTrue(high.addFuel(IronBath.Fuel.COKE, 1));
        assertEquals(1, high.acceptCalcite(1));
        for (int tick = 0; tick < 100; tick++) high.tick(10000, 20000);
        IronBath.Tap slag = high.tap(0, 144);
        assertEquals(IronProduct.SLAG, slag.product());
        assertEquals(32, slag.amountMb());
        assertEquals(0, high.slagMb());
        double highRatio = high.carbon() / high.metalMb();
        assertEquals(
            IronProduct.OVERCARBURIZED_IRON,
            high.tap(0, 144)
                .product());
        assertTrue(high.carbon() / high.metalMb() > highRatio);
    }

    @Test
    void reinforcementHalvesHeatLossAtFullCoverageAndScalesWithCoverage() {
        IronBath ordinary = heated(IronProduct.IRON, 144);
        NBTTagCompound tag = new NBTTagCompound();
        ordinary.save(tag);
        double before = ordinary.heatDemand(ordinary.maximumTemperature());
        ordinary.tick(0, 0);
        double ordinaryLoss = ordinary.heatDemand(ordinary.maximumTemperature()) - before;
        assertTrue(ordinaryLoss > 0);
        for (double coverage : new double[] { 0.25, 0.5, 1 }) {
            IronBath insulated = new IronBath(0);
            insulated.load(tag);
            insulated.setInsulation(coverage);
            insulated.tick(0, 0);
            double loss = insulated.heatDemand(insulated.maximumTemperature()) - before;
            assertEquals(ordinaryLoss * (1 - 0.5 * coverage), loss, 1e-6);
        }
    }

    @Test
    void unreplacedLossTracksCoilsAndTemperatureAfterReload() {
        for (int coilTier : new int[] { 0, 3, 13 }) {
            IronBath original = heated(IronProduct.IRON, 100 * 144);
            assertTrue(original.configure(coilTier));
            original.addHeat(original.heatDemand(original.maximumTemperature()), 0);
            NBTTagCompound tag = new NBTTagCompound();
            original.save(tag);
            IronBath restored = new IronBath(0);
            restored.load(tag);
            restored.cool(0);
            restored.finishTick(false, false);
            double lossEu = restored.heatDemand(restored.maximumTemperature()) * 24000 / 64000;
            assertEquals(60.0 * (1 << coilTier), lossEu, 1e-6);

            original.setInsulation(1);
            original.save(tag);
            restored.load(tag);
            restored.cool(0);
            restored.finishTick(false, false);
            lossEu = restored.heatDemand(restored.maximumTemperature()) * 24000 / 64000;
            assertEquals(30.0 * (1 << coilTier), lossEu, 1e-6);

            double midpoint = (restored.maximumTemperature() + 300) / 2;
            tag.setDouble("Heat", 0);
            tag.setDouble("Insulation", 0);
            restored.load(tag);
            restored.addHeat(restored.heatDemand(midpoint), 0);
            double before = restored.heatDemand(restored.maximumTemperature());
            restored.cool(0);
            restored.finishTick(false, false);
            lossEu = (restored.heatDemand(restored.maximumTemperature()) - before) * 24000 / 64000;
            assertEquals(30.0 * (1 << coilTier), lossEu, 1e-6);
        }
    }

    @Test
    void paidHeatLossKeepsTheBathHotWithoutOverheatingIt() {
        IronBath bath = heated(IronProduct.IRON, 144);
        bath.addHeat(bath.heatDemand(bath.maximumTemperature()), 0);
        double temperature = bath.temperature();
        double replacement = bath.heatLoss();
        bath.cool(replacement);
        bath.finishTick(true, true);
        assertEquals(temperature, bath.temperature());
        bath.cool(replacement * 2);
        bath.finishTick(true, true);
        assertEquals(temperature, bath.temperature());
        bath.cool(0);
        bath.finishTick(false, false);
        assertTrue(bath.temperature() < temperature);
    }

    @Test
    void unpaidHeatLossRemovesTwiceTheEnergyNeededForHotStandby() {
        IronBath bath = heated(IronProduct.IRON, 400 * 144);
        bath.addHeat(bath.heatDemand(bath.maximumTemperature()), 0);
        NBTTagCompound before = new NBTTagCompound();
        bath.save(before);
        double standbyHeat = bath.heatLoss();
        bath.cool(0);
        bath.finishTick(false, false);
        NBTTagCompound after = new NBTTagCompound();
        bath.save(after);
        assertEquals(2 * standbyHeat, before.getDouble("Heat") - after.getDouble("Heat"), 1e-7);
    }

    @Test
    void fluxMakesSlagAndDirtyMetalDoesNotBlockTheUpperSlagTap() {
        IronBath bath = heated(IronProduct.IRON, 200 * 144);
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 6));
        for (int tick = 0; tick < 100; tick++) bath.tick(10000, 20000);
        assertEquals(
            0,
            bath.previewTap(0, 144)
                .amountMb());
        assertEquals(1, bath.acceptCalcite(1));
        bath.tick(1_000_000, 0);
        assertEquals(0, bath.impurities());
        assertEquals(14, bath.calciteCapacity());
        assertEquals(192, bath.slagMb());
        assertTrue(bath.addFuel(IronBath.Fuel.CHARCOAL, 2));
        for (int tick = 0; tick < 100; tick++) bath.tick(10000, 20000);
        assertEquals(
            0,
            bath.previewTap(0, 144)
                .amountMb());
        IronBath.Tap upper = bath.tap(1, 480);
        assertEquals(IronProduct.SLAG, upper.product());
        assertEquals(480, upper.amountMb());
        assertEquals(200 * 144, bath.metalMb());
        assertEquals(160, bath.slagMb());
    }

    @Test
    void pendingMetalReservesItsVolumeBelowTheSlagAllowance() {
        IronBath bath = heated(IronProduct.IRON, 495 * 144);
        assertTrue(bath.addMetal(IronProduct.IRON, 144));
        assertFalse(bath.addMetal(IronProduct.IRON, 144));
        assertTrue(bath.addFuel(IronBath.Fuel.CHARCOAL, 1));
        bath.addHeat(128000, 128000);
        assertEquals(496 * 144, bath.metalMb());
        assertEquals(10, bath.impurities());
        assertEquals(512 * 144, bath.capacityMb());
    }

    @Test
    void fullTankAcceptsCarbonAndWaitsForSlagSpaceWithoutBlockingCleanup() {
        IronBath bath = heated(IronProduct.IRON, 496 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Slag", 16 * 144);
        bath.load(saved);
        bath.addHeat(1_000_000, 0);
        assertTrue(bath.addFuel(IronBath.Fuel.CHARCOAL, 1));
        assertEquals(1, bath.acceptCalcite(1));
        bath.addHeat(64000, 64000);
        bath.cool(0);
        bath.finishTick(true, true);
        assertEquals(10, bath.impurities());
        assertEquals(20, bath.calciteCapacity());
        assertEquals(16 * 144, bath.slagMb());
        assertEquals(
            0,
            bath.previewTap(0, 144)
                .amountMb());
        IronBath.Tap drained = bath.tap(4, 320);
        assertEquals(IronProduct.SLAG, drained.product());
        assertEquals(320, drained.amountMb());
        bath.cool(0);
        bath.finishTick(true, true);
        assertEquals(0, bath.impurities());
        assertEquals(10, bath.calciteCapacity());
        assertEquals(16 * 144, bath.slagMb());
        assertEquals(
            144,
            bath.previewTap(0, 144)
                .amountMb());
    }

    @Test
    void slagBindingKeepsVolumeReservedForQueuedMetal() {
        IronBath bath = heated(IronProduct.IRON, 495 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Slag", 16 * 144);
        saved.setInteger("Impurities", 10);
        saved.setInteger("Calcite", 20);
        bath.load(saved);
        bath.addHeat(1_000_000, 0);
        assertTrue(bath.addMetal(IronProduct.IRON, 144));
        bath.cool(0);
        bath.finishTick(true, true);
        assertEquals(10, bath.impurities());
        assertEquals(16 * 144, bath.slagMb());
        bath.addHeat(64000, 64000);
        assertEquals(496 * 144, bath.metalMb());
        assertEquals(bath.capacityMb(), bath.metalMb() + bath.slagMb());
    }

    @Test
    void savedMetalAboveTheAdmissionLimitCanBeDrainedWithoutChangingCoils() {
        IronBath bath = heated(IronProduct.IRON, 496 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Metal", 512 * 144);
        bath.load(saved);
        bath.addHeat(1_000_000_000, 0);
        assertEquals(512 * 144, bath.capacityMb());
        assertEquals(512 * 144, bath.metalMb());
        assertFalse(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertFalse(bath.addMetal(IronProduct.IRON, 144));
        assertEquals(
            16 * 144,
            bath.tap(0, 16 * 144)
                .amountMb());
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
    }

    @Test
    void metalAllowanceReservesOneThirtySecondAtEveryCoilTier() {
        for (int tier = 0; tier <= 13; tier++) {
            IronBath bath = new IronBath(tier);
            assertEquals(496 * 144 * (1 << tier), bath.metalCapacityMb());
        }
        for (int height = 3; height <= 7; height++) {
            assertEquals(height - 3, IronBathStructure.portBand(height));
        }
    }

    @Test
    void tapBandsSelectOnlyOverlappingLayersAndSwitchAfterSlagIsDrained() {
        IronBath bath = heated(IronProduct.IRON, 20000 / 144 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Metal", 20000);
        saved.setInteger("Slag", 1);
        bath.load(saved);
        bath.addHeat(1_000_000_000, 0);

        assertEquals(
            IronProduct.IRON,
            bath.previewTap(0, 144)
                .product());
        assertEquals(
            IronProduct.SLAG,
            bath.previewTap(1, 144)
                .product());
        assertEquals(
            0,
            bath.previewTap(2, 144)
                .amountMb());
        IronBath.Tap drained = bath.tap(1, 144);
        assertEquals(IronProduct.SLAG, drained.product());
        assertEquals(1, drained.amountMb());
        assertEquals(20000, bath.metalMb());
        assertEquals(
            IronProduct.IRON,
            bath.previewTap(1, 144)
                .product());
        assertEquals(
            0,
            bath.previewTap(-1, 144)
                .amountMb());
        assertEquals(
            0,
            bath.previewTap(5, 144)
                .amountMb());
    }

    @Test
    void slagAtTheTwentyPercentBoundaryBelongsToTheNextBand() {
        IronBath bath = heated(IronProduct.IRON, 100 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Metal", 14745);
        saved.setInteger("Slag", 1);
        bath.load(saved);
        bath.addHeat(1_000_000_000, 0);
        assertEquals(
            IronProduct.IRON,
            bath.previewTap(0, 144)
                .product());
        assertEquals(
            IronProduct.SLAG,
            bath.previewTap(1, 144)
                .product());
    }

    @Test
    void topBandRetainsSlagPriorityAtTheHighestCoilCapacity() {
        IronBath bath = new IronBath(13);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Metal", bath.capacityMb() - 1);
        saved.setInteger("Slag", 1);
        saved.setInteger("Impurities", 10);
        bath.load(saved);
        bath.addHeat(1e20, 0);
        assertEquals(
            IronProduct.SLAG,
            bath.previewTap(4, 144)
                .product());
        assertEquals(
            1,
            bath.tap(4, 144)
                .amountMb());
        assertEquals(10, bath.impurities());
    }

    @Test
    void slagBindingWaitsForWholeUnitsOfFreeVolume() {
        IronBath bath = heated(IronProduct.IRON, 496 * 144);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Slag", 16 * 144 - 47);
        saved.setInteger("Impurities", 2);
        saved.setInteger("Calcite", 20);
        bath.load(saved);
        bath.addHeat(1_000_000, 0);
        bath.cool(0);
        bath.finishTick(true, true);
        assertEquals(1, bath.impurities());
        assertEquals(19, bath.calciteCapacity());
        assertEquals(bath.capacityMb() - 15, bath.metalMb() + bath.slagMb());
        assertEquals(
            17,
            bath.tap(4, 17)
                .amountMb());
        bath.cool(0);
        bath.finishTick(true, true);
        assertEquals(0, bath.impurities());
        assertEquals(bath.capacityMb(), bath.metalMb() + bath.slagMb());
    }

    @Test
    void queuedFuelCannotOverflowTheImpurityCounter() {
        IronBath bath = new IronBath(0);
        NBTTagCompound saved = new NBTTagCompound();
        bath.save(saved);
        saved.setInteger("Impurities", Integer.MAX_VALUE - 10);
        bath.load(saved);
        assertTrue(bath.addFuel(IronBath.Fuel.CHARCOAL, 1));
        assertFalse(bath.addFuel(IronBath.Fuel.COKE, 1));
        bath.addHeat(64000, 64000);
        assertEquals(Integer.MAX_VALUE, bath.impurities());
        assertFalse(bath.addFuel(IronBath.Fuel.COKE, 1));
    }

    @Test
    void carbonCapacityUsesTwentyCarbonPerIngotIncludingQueuedCharges() {
        IronBath bath = new IronBath(0);
        NBTTagCompound tag = new NBTTagCompound();
        bath.save(tag);
        tag.setDouble("Carbon", 512 * 20 - 100);
        bath.load(tag);
        assertTrue(bath.addFuel(IronBath.Fuel.COKE, 1));
        assertFalse(bath.addFuel(IronBath.Fuel.CACTUS_COKE, 1));
        assertFalse(bath.addMetal(IronProduct.STEEL, 144));
        assertTrue(bath.addMetal(IronProduct.IRON, 144));
    }

    @Test
    void oxygenFlowStaysConstantWhenCarbonRunsOut() {
        IronBath bath = heated(IronProduct.PIG_IRON, 144);
        double before = bath.carbon();
        assertEquals(32, bath.addOxygen(32));
        assertEquals(before - 1, bath.carbon(), 1e-9);
        assertEquals(1000, bath.addOxygen(1000));
        assertEquals(0, bath.carbon());
        assertEquals(32, bath.addOxygen(32));
        assertEquals(0, bath.carbon());
        assertEquals(0, bath.impurities());
        assertEquals(144, bath.metalMb());
    }

    @Test
    void oxygenPassesThroughCarbonFreeLiquidWithoutChangingTheBath() {
        IronBath bath = heated(IronProduct.IRON, 144);
        double temperature = bath.temperature();
        for (int tick = 0; tick < 20; tick++) assertEquals(15, bath.addOxygen(15));
        assertEquals(0, bath.carbon());
        assertEquals(0, bath.impurities());
        assertEquals(0, bath.slagMb());
        assertEquals(144, bath.metalMb());
        assertEquals(144, bath.liquidMetalMb());
        assertEquals(temperature, bath.temperature());
        assertEquals(0, bath.addOxygen(0));
        assertEquals(0, bath.addOxygen(-1));
    }

    @Test
    void oxygenPassesThroughColdContentsWithoutReacting() {
        IronBath bath = heated(IronProduct.PIG_IRON, 144);
        for (int tick = 0; tick < 25000; tick++) bath.tick(0, 0);
        assertEquals(0, bath.liquidMetalMb());
        double before = bath.carbon();
        assertEquals(32, bath.addOxygen(32));
        assertEquals(before, bath.carbon());
        assertEquals(144, bath.metalMb());
        assertEquals(32, new IronBath(0).addOxygen(32));
    }

    @Test
    void coldContentsRemainAndCanBeRemeltedAfterPowerLoss() {
        IronBath bath = heated(IronProduct.STEEL, 144);
        for (int tick = 0; tick < 25000; tick++) bath.tick(0, 0);
        assertEquals(144, bath.metalMb());
        assertEquals(0, bath.liquidMetalMb());
        assertEquals(
            0,
            bath.previewTap(0, 144)
                .amountMb());
        for (int tick = 0; tick < 100; tick++) bath.tick(10000, 10000);
        assertEquals(144, bath.liquidMetalMb());
    }

    @Test
    void partiallyHeatedChargeCoolsDuringAnUnpoweredShutdown() {
        IronBath cold = new IronBath(0);
        IronBath shutdown = new IronBath(0);
        assertTrue(cold.addMetal(IronProduct.IRON, 144));
        assertTrue(shutdown.addMetal(IronProduct.IRON, 144));
        for (int tick = 0; tick < 9; tick++) shutdown.tick(1000, 500);
        assertTrue(shutdown.heatDemand(1800) < cold.heatDemand(1800));
        for (int tick = 0; tick < 300000; tick++) shutdown.tick(0, 0);
        assertEquals(144, shutdown.pendingMetalMb());
        assertEquals(cold.heatDemand(1800), shutdown.heatDemand(1800), 1);
    }

    @Test
    void unpoweredHeatExchangeCannotReduceTheRemainingEnergyDemand() {
        IronBath bath = heated(IronProduct.IRON, 8 * 144);
        assertTrue(bath.addMetal(IronProduct.PIG_IRON, 144));
        double previousDemand = bath.heatDemand(IronBath.MAX_TEMPERATURE);
        for (int tick = 0; tick < 1000; tick++) {
            bath.tick(0, 0);
            double demand = bath.heatDemand(IronBath.MAX_TEMPERATURE);
            assertTrue(demand >= previousDemand - 1e-6);
            previousDemand = demand;
        }
    }

    @Test
    void saveRestoresPartiallyHeatedChargeAndTheSubsequentProcess() {
        IronBath original = new IronBath(0);
        assertTrue(original.addMetal(IronProduct.IRON, 3 * 144));
        assertTrue(original.addMetal(IronProduct.PIG_IRON, 3 * 144));
        assertTrue(original.addFuel(IronBath.Fuel.COAL, 1));
        assertEquals(1, original.acceptCalcite(1));
        for (int tick = 0; tick < 9; tick++) original.tick(1000, 500);
        NBTTagCompound tag = new NBTTagCompound();
        original.save(tag);
        IronBath restored = new IronBath(0);
        restored.load(tag);
        assertEquals(original.temperature(), restored.temperature());
        assertEquals(original.pendingMetalMb(), restored.pendingMetalMb());
        for (int tick = 0; tick < 500; tick++) {
            original.tick(10000, 20000);
            restored.tick(10000, 20000);
        }
        assertEquals(original.metalMb(), restored.metalMb());
        assertEquals(original.slagMb(), restored.slagMb());
        assertEquals(original.carbon(), restored.carbon());
        assertEquals(original.temperature(), restored.temperature());
        assertEquals(original.heatDemand(1800), restored.heatDemand(1800));
    }

    private static IronBath heated(IronProduct product, int amount) {
        IronBath bath = new IronBath(0);
        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(8 * 144, remaining);
            assertTrue(bath.addMetal(product, batch));
            for (int tick = 0; tick < 100; tick++) bath.tick(10000, 20000);
            remaining -= batch;
        }
        for (int tick = 0; tick < 100; tick++) bath.tick(10000, 20000);
        assertEquals(amount, bath.metalMb());
        assertEquals(amount, bath.liquidMetalMb());
        return bath;
    }
}
