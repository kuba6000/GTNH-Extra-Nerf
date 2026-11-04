package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.client.ClientTestRuntime;

import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionData;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Runs native integration fixtures as separately reported scratch-world scenarios. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class NativeScenarios {

    private NativeScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void healingAxe(GameTestHelper helper) {
        scenario(helper, "healingAxe", false, () -> HealingAxeIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void recipes(GameTestHelper helper) {
        scenario(helper, "recipes", false, () -> MixinRecipeIntegrationTests.verifyRecipes());
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void steamDisplays(GameTestHelper helper) {
        scenario(
            helper,
            "steamDisplays",
            false,
            () -> SteamConsumptionIntegrationTests.verifyDisplays(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void steamGrinder(GameTestHelper helper) {
        scenario(
            helper,
            "steamGrinder",
            false,
            () -> SteamConsumptionIntegrationTests.verifyGrinder(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void steamBoilers(GameTestHelper helper) {
        scenario(helper, "steamBoilers", false, () -> SteamConsumptionIntegrationTests.verifyBoiler(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void steamMachines(GameTestHelper helper) {
        scenario(
            helper,
            "steamMachines",
            false,
            () -> SteamConsumptionIntegrationTests.verifySingleMachine(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void singleblockPowerFailure(GameTestHelper helper) {
        scenario(
            helper,
            "singleblockPowerFailure",
            true,
            () -> SingleblockPowerFailureIntegrationTests.verifyElectrical(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void steamPowerFailure(GameTestHelper helper) {
        scenario(
            helper,
            "steamPowerFailure",
            true,
            () -> SingleblockPowerFailureIntegrationTests.verifySteam(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void certifiedPowerFailure(GameTestHelper helper) {
        scenario(
            helper,
            "certifiedPowerFailure",
            true,
            () -> SingleblockPowerFailureIntegrationTests.verifyCertification(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void fluidPowerFailure(GameTestHelper helper) {
        scenario(
            helper,
            "fluidPowerFailure",
            true,
            () -> SingleblockPowerFailureIntegrationTests.verifyFluidAndReload(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void steamPump(GameTestHelper helper) {
        scenario(helper, "steamPump", false, () -> SteamConsumptionIntegrationTests.verifyPump(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void hatchCertification(GameTestHelper helper) {
        scenario(
            helper,
            "hatchCertification",
            false,
            () -> HatchCertificationIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void handChisel(GameTestHelper helper) {
        scenario(
            helper,
            "handChisel",
            false,
            () -> CasingFinishingIntegrationTests.verifyHandChisel(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void spatialAssembly(GameTestHelper helper) {
        scenario(helper, "spatialAssembly", true, () -> SpatialAssemblyIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void lightningRod(GameTestHelper helper) {
        scenario(helper, "lightningRod", true, () -> LightningRodIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void transformers(GameTestHelper helper) {
        scenario(helper, "transformers", true, () -> TransformerIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void legacyFusion(GameTestHelper helper) {
        scenario(helper, "legacyFusion", true, () -> LegacyFusionIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void plasmaForge(GameTestHelper helper) {
        scenario(helper, "plasmaForge", true, () -> PlasmaForgeIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void coolantTower(GameTestHelper helper) {
        scenario(helper, "coolantTower", true, () -> CoolantTowerIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void massFabricator(GameTestHelper helper) {
        scenario(helper, "massFabricator", true, () -> MassFabricatorIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void p2pChannels(GameTestHelper helper) {
        scenario(helper, "p2pChannels", true, () -> P2PChannelIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void brainrotter(GameTestHelper helper) {
        scenario(helper, "brainrotter", true, () -> BrainrotterIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void voidMiner(GameTestHelper helper) {
        scenario(helper, "voidMiner", true, () -> VoidMinerFluidIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void ozonation(GameTestHelper helper) {
        scenario(helper, "ozonation", true, () -> OzonationIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void certificationLifecycle(GameTestHelper helper) {
        scenario(helper, "exam payments, global progression and Steam accidents", false, () -> {
            ExamIntegrationTests.verify(helper.getWorld());
            ProgressionIntegrationTests.verify(helper.getWorld());
            SteamIntegrationTests.verify(helper.getWorld());
        });
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void everglades(GameTestHelper helper) {
        scenario(helper, "everglades", false, () -> EvergladesIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void physicalCircuits(GameTestHelper helper) {
        scenario(
            helper,
            "physicalCircuits",
            false,
            () -> PhysicalCircuitDropIntegrationTests.verify(helper.getWorld()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void loot(GameTestHelper helper) {
        scenario(helper, "loot", false, () -> LootOpeningIntegrationTests.verify(MinecraftServer.getServer()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void telemetry(GameTestHelper helper) {
        scenario(helper, "telemetry", false, () -> TelemetryIntegrationTests.verify(MinecraftServer.getServer()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void teamNotice(GameTestHelper helper) {
        scenario(helper, "teamNotice", false, () -> TeamNoticeIntegrationTests.verify(MinecraftServer.getServer()));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void teamLeader(GameTestHelper helper) {
        scenario(helper, "teamLeader", false, () -> TeamLeaderIntegrationTests.verify(MinecraftServer.getServer()));
    }

    static void scenario(GameTestHelper helper, String name, boolean powered, Fixture fixture) {
        ClientTestRuntime.setMobsEnabled(true);
        helper.afterTest(() -> ClientTestRuntime.setMobsEnabled(false));
        helper.startSequence()
            .thenExecute(name, () -> {
                ProgressionData data = (ProgressionData) helper.getWorld().mapStorage
                    .loadData(ProgressionData.class, ProgressionData.NAME);
                if (data == null) {
                    data = new ProgressionData();
                    helper.getWorld().mapStorage.setData(ProgressionData.NAME, data);
                }
                NBTTagCompound saved = new NBTTagCompound();
                data.writeToNBT(saved);
                if (powered) for (ProgressionTier tier : ProgressionTier.values())
                    MachineProgression.certify(helper.getWorld(), tier);
                try {
                    fixture.verify();
                } catch (Exception failure) {
                    throw new IllegalStateException("Native fixture failed: " + name, failure);
                } finally {
                    data.readFromNBT(saved);
                    data.markDirty();
                }
            })
            .thenSucceed();
    }

    @FunctionalInterface
    interface Fixture {

        void verify() throws Exception;
    }
}
