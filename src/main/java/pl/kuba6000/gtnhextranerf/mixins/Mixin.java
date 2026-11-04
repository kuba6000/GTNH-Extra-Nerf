package pl.kuba6000.gtnhextranerf.mixins;

import static com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer.Phase.EARLY;
import static com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer.Phase.LATE;
import static pl.kuba6000.gtnhextranerf.mixins.TargetedMod.GT5U;
import static pl.kuba6000.gtnhextranerf.mixins.TargetedMod.GT6;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixin implements IMixins {

    ArcaneCraftingRemainders(new MixinBuilder().addCommonMixins("MixinArcaneCraftingRemainders")
        .addRequiredMod(TargetedMod.THAUMCRAFT)
        .setPhase(LATE)),

    SoybeanCrop(new MixinBuilder().addCommonMixins("MixinSoybeanCrop")
        .addRequiredMod(TargetedMod.HARVESTCRAFT)
        .setPhase(LATE)),

    SteamInputHatch(new MixinBuilder().addCommonMixins("MixinSteamInputHatch")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    AssemblyLineDimension(new MixinBuilder().addCommonMixins("MixinAssemblyLineDimension")
        .addRequiredMod(GT5U)
        .addRequiredMod(TargetedMod.APPLIED_ENERGISTICS_2)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    LightningRod(new MixinBuilder().addCommonMixins("MixinLightningRod")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    VoidMinerFluids(new MixinBuilder().addCommonMixins("MixinVoidMinerFluids")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    EnergyPorts(new MixinBuilder().addCommonMixins("MixinEnergyPorts")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    EvergladesMachineBan(new MixinBuilder().addCommonMixins("MixinEvergladesMachineBan")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    MassFabricatorAmplifier(new MixinBuilder().addCommonMixins("MixinMassFabricatorAmplifier")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    CoolantTower(new MixinBuilder().addCommonMixins("MixinCoolantTower")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    PlasmaForgeCoils(new MixinBuilder().addCommonMixins("MixinPlasmaForgeCoils")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    OzonationSafety(new MixinBuilder().addCommonMixins("MixinOzonationSafety", "MixinOzonationExplosion")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    LegacyFusionCoils(new MixinBuilder().addCommonMixins("MixinLegacyFusionCoils")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    ProgressionCertification(new MixinBuilder()
        .addCommonMixins(
            "MixinProgressionTickGate",
            "MixinWirelessHatchCertification",
            "MixinProgressionMachineStatus",
            "MixinProgressionMachineGui",
            "MixinElectricalCableFire",
            "MixinElectricalMachineExplosion",
            "MixinSteamPipeDamage",
            "MixinSteamExhaustDamage")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    HungryAxe(new MixinBuilder().addCommonMixins("MixinHealingAxeHunger")
        .addRequiredMod(TargetedMod.EXTRA_UTILITIES)
        .setPhase(LATE)),

    LootBags(new MixinBuilder().addCommonMixins("MixinItemLootBag", "MixinLootBagCommand", "MixinLootGroupsHandler")
        .addRequiredMod(TargetedMod.ENHANCED_LOOT_BAGS)
        .setPhase(LATE)),

    LootBagPreview(new MixinBuilder().addClientMixins("MixinLootBagPreview")
        .addRequiredMod(TargetedMod.ENHANCED_LOOT_BAGS)
        .addRequiredMod(TargetedMod.NOT_ENOUGH_ITEMS)
        .setPhase(LATE)),

    CraftingTaskRetrieval(new MixinBuilder().addCommonMixins("MixinCraftingTaskRetrieval")
        .addClientMixins("MixinCraftingTaskRetrievalGui")
        .addRequiredMod(TargetedMod.BETTER_QUESTING)
        .addRequiredMod(TargetedMod.BQ_STANDARD)
        .setPhase(LATE)),

    StartupErrorScreen(new MixinBuilder().addClientMixins("MixinFMLClientHandler")
        .setPhase(EARLY)),
    CraftingManagerAccessor(new MixinBuilder().addCommonMixins("MixinCraftingManagerAccessor")
        .setPhase(EARLY)),
    RecipeAccessor(new MixinBuilder().addCommonMixins("MixinRecipeAccessor")
        .setPhase(EARLY)),
    ShapedOreRecipeAccessor(new MixinBuilder().addCommonMixins("MixinShapedOreRecipeAccessor")
        .setPhase(EARLY)),
    SmallOres(new MixinBuilder().addCommonMixins("MixinSmallOres")
        .setPhase(EARLY)),
    GTBlockFluid(new MixinBuilder().addCommonMixins("MixinLoaderGTBlockFluid")
        .setPhase(EARLY)),
    Ae2Crafting(new MixinBuilder().addCommonMixins("MixinAe2Crafting")
        .addRequiredMod(GT5U)
        .addRequiredMod(TargetedMod.DREAMCRAFT)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    WirelessConnectionPower(new MixinBuilder().addCommonMixins("MixinWirelessConnectionPower")
        .addRequiredMod(TargetedMod.APPLIED_ENERGISTICS_2)
        .setPhase(LATE)),
    MeP2PChannelCapacity(new MixinBuilder().addCommonMixins("MixinMeP2PChannelCapacity")
        .addRequiredMod(TargetedMod.APPLIED_ENERGISTICS_2)
        .setPhase(LATE)),
    SteamTurbine(new MixinBuilder().addCommonMixins("MixinMTESteamTurbine")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    PlasmaGenerator(new MixinBuilder().addCommonMixins("MixinMTEPlasmaGenerator")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    SteamOverclock(new MixinBuilder()
        .addCommonMixins(
            "MixinSteamOverclockDescriber",
            "MixinSteamMachineTank",
            "MixinSteamMachineDisplay",
            "MixinSteamMachineGui")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    SteamMultiBlock(new MixinBuilder().addCommonMixins("MixinSteamMultiBlock", "MixinSteamMultiBlockWailaData")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    DieselGenerator(new MixinBuilder().addCommonMixins("MixinMTEDieselGenerator")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    GasTurbine(new MixinBuilder().addCommonMixins("MixinMTEGasTurbine")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    LargeChemicalReactor(new MixinBuilder().addCommonMixins("MixinMTELargeChemicalReactor")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    MetaTileEntity(new MixinBuilder().addCommonMixins("MixinMetaTileEntity")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    ConfigurationCircuitSupport(new MixinBuilder()
        .addCommonMixins(
            "MixinConfigurationCircuitSupport",
            "MixinPhysicalCircuitBlockDrop",
            "MixinPhysicalCircuitExplosionDrop")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    CraftingMaterial(new MixinBuilder().addCommonMixins("MixinCraftingMaterialLoader")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    SemiFluidGen(new MixinBuilder().addCommonMixins("MixinMTESemiFluidGenerator")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    ProcessingWire(new MixinBuilder().addCommonMixins("MixinProcessingWire")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    CircuitAssembler(new MixinBuilder().addCommonMixins("MixinCircuitAssemblerRecipes")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    MetaGeneratedItem01(new MixinBuilder().addCommonMixins("MixinMetaGeneratedItem01")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    MaterialFuelValues(new MixinBuilder().addCommonMixins("MixinMaterialsInit")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    BrainrotMoltenAudit(new MixinBuilder().addCommonMixins("MixinGodforge")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    SafeUnificationAlternatives(new MixinBuilder().addCommonMixins("MixinGTOreDictUnificator")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    ProcessingShaping(new MixinBuilder().addCommonMixins("MixinProcessingShaping")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),
    AssemblyLine(new MixinBuilder().addCommonMixins("MixinAssemblyLineRecipes")
        .addRequiredMod(GT5U)
        .addExcludedMod(GT6)
        .setPhase(LATE)),

    ;

    private final MixinBuilder builder;

    Mixin(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    @NotNull
    public MixinBuilder getBuilder() {
        return builder;
    }

}
