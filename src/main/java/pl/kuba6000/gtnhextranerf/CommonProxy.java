package pl.kuba6000.gtnhextranerf;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.widget.Widget;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import drethic.questbook.config.QBConfig;
import gregtech.api.GregTechAPI;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditReport;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.items.BlightedSoybean;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedRods;
import pl.kuba6000.gtnhextranerf.loader.ComponentLoader;
import pl.kuba6000.gtnhextranerf.loader.LootLoader;
import pl.kuba6000.gtnhextranerf.loader.RecipeLoader;
import pl.kuba6000.gtnhextranerf.loot.runtime.LootAllowanceSync;
import pl.kuba6000.gtnhextranerf.loot.runtime.ServerLootRuntime;
import pl.kuba6000.gtnhextranerf.machines.Brainrotter;
import pl.kuba6000.gtnhextranerf.machines.ProgressionMachineNerf;
import pl.kuba6000.gtnhextranerf.machines.RawMachineCasings;
import pl.kuba6000.gtnhextranerf.materials.NerfMaterials;
import pl.kuba6000.gtnhextranerf.progression.CertificationCost;
import pl.kuba6000.gtnhextranerf.progression.ElectricalSafety;
import pl.kuba6000.gtnhextranerf.progression.EnergyHatchCertification;
import pl.kuba6000.gtnhextranerf.progression.ProgressionCertificates;
import pl.kuba6000.gtnhextranerf.progression.ProgressionServerTasks;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import pl.kuba6000.gtnhextranerf.progression.SteamSafety;
import pl.kuba6000.gtnhextranerf.progression.exam.CertificationExams;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamSyncHandler;
import pl.kuba6000.gtnhextranerf.recipes.MetalDuplication;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;
import pl.kuba6000.gtnhextranerf.recipes.crafting.ArcaneWorkbenchSync;
import pl.kuba6000.gtnhextranerf.teams.TeamCommand;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;
import pl.kuba6000.gtnhextranerf.telemetry.ConsentGui;
import pl.kuba6000.gtnhextranerf.telemetry.ConsentPrompt;
import pl.kuba6000.gtnhextranerf.telemetry.ServerTelemetry;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryCommand;
import pl.kuba6000.gtnhextranerf.update.UpdateChecker;

public class CommonProxy {

    private static final String INTEGRITY_FAILURE_PREFIX = "GTNH Extra Nerf integrity check failed:";

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        LootAllowanceSync.initialize();
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ServerTelemetry.initialize(event.getSourceFile(), GTNHExtraNerf.isDevelopmentEnvironment());
        TeamTracking.initialize(event.getSourceFile(), GTNHExtraNerf.isDevelopmentEnvironment());
        ProgressionMachineNerf.registerRecipeMaps();
        NerfMaterials.registerBlocks();
        RawMachineCasings.register();
        Brainrotter.registerItem();
        BlightedSoybean.register();
        MagicnetizedRods.register();
        FMLCommonHandler.instance()
            .bus()
            .register(ArcaneWorkbenchSync.INSTANCE);
        ComponentLoader.register();
        ProgressionCertificates.register();
        CertificationExams.register(event.getModConfigurationDirectory());
        ConsentGui.register();
        MinecraftForge.EVENT_BUS.register(new EnergyHatchCertification());
        FMLCommonHandler.instance()
            .bus()
            .register(new ProgressionServerTasks());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        Brainrotter.registerMachine();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        runStartupStep(RawMachineCasings::registerGroups);
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        // GT can receive LoadComplete after us. NHCore's Space Assembler recipes are added in this queue.
        if (GregTechAPI.sGTCompleteLoad != null) {
            GregTechAPI.sGTCompleteLoad.add(this::finishRecipeLoading);
        } else {
            finishRecipeLoading();
        }
    }

    private void finishRecipeLoading() {
        Throwable startupFailure = null;
        try {
            if (AuditScope.VERIFY_PARTY_REWARDS.isActive()) runStartupStep(() -> {
                AuditManager.run(AuditScope.VERIFY_PARTY_REWARDS, audit -> {
                    if (!QBConfig.syncQuestRewards) throw new IllegalStateException("Party reward sharing is disabled");
                    audit.success();
                });
            });
            runStartupStep(NerfMaterials::verify);
            runStartupStep(ProgressionCertificates::verify);
            runStartupStep(EnergyHatchCertification::verify);
            runStartupStep(ElectricalSafety::verify);
            runStartupStep(SteamSafety::verify);
            runStartupStep(CertificationExams::verify);
            runStartupStep(CertificationCost::verify);
            runStartupStep(MagicnetizedRods::registerArcaneRecipe);
            if (AuditScope.MODIFY_OTHER_METAL_TRANSMUTATIONS.isActive()) runStartupStep(MetalDuplication::verify);
            runStartupStep(RecipeLoader::run);
            runStartupStep(RawMachineCasings::verify);
            runStartupStep(ProgressionMachineNerf::verifyFusion);
            runStartupStep(ProgressionMachineNerf::verifyOzone);
            runStartupStep(ProgressionMachineNerf::verifyPlasmaForge);
            runStartupStep(ProgressionMachineNerf::verifyCoolantTower);
            runStartupStep(ProgressionMachineNerf::verifyMassFabricators);
            runStartupStep(ProgressionMachineNerf::verifyEverglades);
            runStartupStep(ProgressionMachineNerf::verifyTransformerPorts);
            runStartupStep(ProgressionMachineNerf::verifyVoidMinerFluids);
            runStartupStep(ProgressionMachineNerf::verifyLightningRods);
            if (AuditScope.VERIFY_ASSEMBLY_LINE_SPATIAL_DIMENSION.isActive())
                runStartupStep(ProgressionMachineNerf::verifySpatialAssemblyLines);
            runStartupStep(Brainrotter::verify);
            if (AuditScope.VERIFY_NATIVE_LOOT_TABLES.isActive()) runStartupStep(LootLoader::verifyPublished);
        } catch (RuntimeException | Error failure) {
            startupFailure = failure;
        }

        AuditReport report = null;
        try {
            report = AuditManager.finish();
        } catch (RuntimeException | Error finishFailure) {
            if (startupFailure == null) startupFailure = finishFailure;
            else startupFailure.addSuppressed(finishFailure);
        }
        if (report != null) {
            if (report.accepted() && startupFailure == null) GTNHExtraNerf.LOG.info(report.summary());
            else {
                GTNHExtraNerf.LOG.warn(report.summary());
                if (!report.accepted()) {
                    GTNHExtraNerf.LOG.warn(report.failureReport());
                    if (!GTNHExtraNerf.isDevelopmentEnvironment() && startupFailure == null)
                        startupFailure = new IllegalStateException(report.failureReport());
                }
            }
        }
        if (startupFailure != null) {
            String detail = startupFailure.getMessage() == null ? startupFailure.getClass()
                .getName() : startupFailure.getMessage();
            RuntimeException integrityFailure = new IllegalStateException(
                detail.startsWith(INTEGRITY_FAILURE_PREFIX) ? detail : INTEGRITY_FAILURE_PREFIX + "\n- " + detail,
                startupFailure);
            GTNHExtraNerf.LOG.fatal(integrityFailure.getMessage(), integrityFailure);
            handleIntegrityFailure(integrityFailure);
        }
    }

    private static void runStartupStep(Runnable step) {
        try {
            step.run();
        } catch (RecipeValidationException failure) {
            if (!GTNHExtraNerf.isDevelopmentEnvironment()) throw failure;
            GTNHExtraNerf.LOG.warn("Development registry validation failed; continuing startup", failure);
        }
    }

    protected void handleIntegrityFailure(RuntimeException failure) {
        FMLCommonHandler.instance()
            .raiseException(failure, failure.getMessage(), true);
        throw failure;
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        ServerLootRuntime.start(event.getServer());
        // The decisions about telemetry and teams belong to the world, so they are read before anything starts.
        Config.loadWorld();
        ServerTelemetry.startServer(event.getServer());
        TeamTracking.start(event.getServer());
        ConsentPrompt.start(event.getServer(), GTNHExtraNerf.isDevelopmentEnvironment());
        UpdateChecker.start(Tags.VERSION);
        event.registerServerCommand(new TelemetryCommand());
        event.registerServerCommand(new TeamCommand());
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        try {
            ServerLootRuntime.stop();
        } finally {
            ConsentPrompt.stop();
            ServerTelemetry.stopServer();
            TeamTracking.stop();
            UpdateChecker.stop();
            ProgressionServerTasks.reset();
            ArcaneWorkbenchSync.reset();
            Config.unloadWorld();
        }
    }

    /** Opens a web page for the player; there is nobody to show it to on a dedicated server. */
    public void openUrl(String url) {}

    public void showCertificate(ProgressionTier tier) {}

    public void receiveControllerAllowance(int remaining, MessageContext context) {}

    public void openExam(ProgressionTier tier) {}

    public Widget<?> createExamCanvas(ExamSyncHandler exam) {
        return new Widget<>().size(270, 138);
    }
}
