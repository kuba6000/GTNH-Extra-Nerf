package pl.kuba6000.gtnhextranerf.loader.recipes;

import static com.dreammaster.scripts.IngredientFactory.getModItem;
import static gregtech.api.enums.Mods.Forestry;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.circuitAssemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFurnaceRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeBuilder.TICKS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.dreammaster.item.NHItemList;

import bartworks.system.material.WerkstoffLoader;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.machines.RawMachineCasings;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

public final class CoreModRecipeNerf implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.REMOVE_IRON_LV_MOTOR_ASSEMBLER_RECIPE, CoreModRecipeNerf::removeIronLvMotor);
        registry.register(AuditScope.MODIFY_SUPERCONDUCTOR_COIL_RECIPE, CoreModRecipeNerf::modifySuperconductorCoil);
        registry.register(
            AuditScope.MODIFY_LARGE_CHEMICAL_REACTOR_ASSEMBLER_RECIPE,
            CoreModRecipeNerf::modifyLargeChemicalReactor);
        registry.register(AuditScope.MODIFY_ELITE_CIRCUIT_RECIPES, CoreModRecipeNerf::modifyEliteCircuit);
        registry.register(AuditScope.MODIFY_PPIC_WAFER_RECIPES, CoreModRecipeNerf::modifyPpicWafer);
        registry.register(AuditScope.REMOVE_DIODE_ASSEMBLER_RECIPES, CoreModRecipeNerf::removeDiodeRecipes);
        registry.register(AuditScope.MODIFY_DIAMOND_SAWBLADE_RECIPE, CoreModRecipeNerf::modifyDiamondSawblade);
        registry.register(AuditScope.MODIFY_MACHINE_CASING_RECIPES, CoreModRecipeNerf::modifyMachineCasings);
        registry.register(AuditScope.MODIFY_ADVANCED_CIRCUIT_INPUTS, CoreModRecipeNerf::modifyAdvancedCircuitInputs);
        registry.register(
            AuditScope.MODIFY_INTEGRATED_LOGIC_CIRCUIT_INPUTS,
            CoreModRecipeNerf::modifyIntegratedLogicCircuitInputs);
        registry.register(AuditScope.MODIFY_GOOD_CIRCUIT_INPUTS, CoreModRecipeNerf::modifyGoodCircuitInputs);
        registry.register(AuditScope.MODIFY_CUPRONICKEL_COIL_INPUTS, CoreModRecipeNerf::modifyCupronickelCoilInputs);
        registry.register(AuditScope.MODIFY_MV_ENERGY_HATCH_INPUTS, CoreModRecipeNerf::modifyMvEnergyHatchInputs);
        registry.register(AuditScope.MODIFY_RAW_NEUTRONIUM_RECIPE, CoreModRecipeNerf::modifyRawNeutronium);
        registry.register(AuditScope.MODIFY_INDIUM_COMB_RECIPES, CoreModRecipeNerf::modifyIndiumCombRecipes);
        registry.register(
            AuditScope.MODIFY_PRASEODYMIUM_CENTRIFUGE_RECIPE,
            CoreModRecipeNerf::modifyPraseodymiumCentrifuge);
        registry
            .register(AuditScope.MODIFY_VACUUM_FURNACE_INDIUM_RECIPES, CoreModRecipeNerf::modifyVacuumFurnaceIndium);
    }

    private static void removeIronLvMotor(RecipeContext recipes) {
        recipes.machine(assemblerRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.stick, Materials.IronMagnetic, 1),
                        GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 2))
                    .oreDictInput("stickAnyIron", 2)
                    .oreDictInput("wireGt01AnyCopper", 4)
                    .itemOutputs(ItemList.Electric_Motor_LV.get(1))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(20 * TICKS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                1);
    }

    private static void modifySuperconductorCoil(RecipeContext recipes) {
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorLuV, 64),
                        NHItemList.MicaInsulatorFoil.get(48))
                    .itemOutputs(ItemList.Casing_Coil_Superconductor.get(1))
                    .fluidInputs(Materials.Trinium.getMolten(1_440))
                    .fluidOutputs()
                    .duration(50 * SECONDS)
                    .eut(9001)
                    .build(),
                RecipeChange.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.wireGt08, Materials.SuperconductorLuV, 64),
                        NHItemList.MicaInsulatorFoil.get(192))
                    .build());
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorZPM, 32),
                        ItemList.Naquarite_Universal_Insulator_Foil.get(4))
                    .itemOutputs(ItemList.Casing_Coil_Superconductor.get(1))
                    .fluidInputs(Materials.Trinium.getMolten(576))
                    .fluidOutputs()
                    .duration(50 * SECONDS)
                    .eut(9001)
                    .build(),
                RecipeChange.builder()
                    .replaceItemInput(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SuperconductorZPM, 32),
                        GTOreDictUnificator.get(OrePrefixes.wireGt04, Materials.SuperconductorZPM, 32))
                    .itemInputAmount(ItemList.Naquarite_Universal_Insulator_Foil.get(1), 8)
                    .build());
    }

    private static void modifyLargeChemicalReactor(RecipeContext recipes) {
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        ItemList.Electric_Motor_HV.get(1),
                        ItemList.Hull_HV.get(1),
                        GTOreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Polytetrafluoroethylene, 2),
                        GTOreDictUnificator.get(OrePrefixes.circuit, Materials.HV, 4),
                        GTOreDictUnificator.get(OrePrefixes.rotor, Materials.StainlessSteel, 1),
                        GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(ItemList.Machine_Multi_LargeChemicalReactor.get(1))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_HV)
                    .build(),
                RecipeChange.builder()
                    .itemInputs(
                        ItemList.Electric_Motor_HV.get(1),
                        ItemList.Hull_HV.get(1),
                        GTOreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Polytetrafluoroethylene, 2),
                        GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 4),
                        GTOreDictUnificator.get(OrePrefixes.rotor, Materials.StainlessSteel, 1),
                        GTUtility.getIntegratedCircuit(1))
                    .build());
    }

    private static void modifyEliteCircuit(RecipeContext recipes) {
        ItemStack[] standardInputs = { GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Aluminium, 2),
            ItemList.Circuit_Data.get(2), ItemList.Circuit_Parts_Coil.get(12), ItemList.Circuit_Parts_Capacitor.get(24),
            ItemList.Circuit_Chip_Ram.get(16),
            GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.AnnealedCopper, 24) };
        ItemStack[] asmdInputs = { GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Aluminium, 2),
            ItemList.Circuit_Data.get(2), ItemList.Circuit_Parts_InductorASMD.get(3),
            ItemList.Circuit_Parts_CapacitorASMD.get(4), ItemList.Circuit_Chip_Ram.get(16),
            GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.AnnealedCopper, 16) };

        removeOldEliteSolderRecipes(recipes, standardInputs, 80 * SECONDS);
        removeOldEliteSolderRecipes(recipes, asmdInputs, 40 * SECONDS);

        RecipeMatch standardTarget = RecipeMatch.builder()
            .itemInputs(standardInputs)
            .itemOutputs(ItemList.Circuit_Elite.get(1))
            .fluidInputs(Materials.SoulInfusedMedium.getFluid(100))
            .fluidOutputs()
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .build();
        RecipeMatch asmdTarget = RecipeMatch.builder()
            .itemInputs(asmdInputs)
            .itemOutputs(ItemList.Circuit_Elite.get(1))
            .fluidInputs(Materials.SoulInfusedMedium.getFluid(100))
            .fluidOutputs()
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .build();
        recipes.machine(circuitAssemblerRecipes)
            .removeExactly(standardTarget, 0);
        recipes.machine(circuitAssemblerRecipes)
            .removeExactly(asmdTarget, 0);

        recipes.machine(circuitAssemblerRecipes)
            .addOne(
                GTValues.RA.stdBuilder()
                    .itemInputs(standardInputs)
                    .itemOutputs(ItemList.Circuit_Elite.get(1))
                    .fluidInputs(Materials.SoulInfusedMedium.getFluid(100))
                    .requiresCleanRoom()
                    .duration(80 * SECONDS)
                    .eut(TierEU.RECIPE_HV),
                standardTarget);
        recipes.machine(circuitAssemblerRecipes)
            .addOne(
                GTValues.RA.stdBuilder()
                    .itemInputs(asmdInputs)
                    .itemOutputs(ItemList.Circuit_Elite.get(1))
                    .fluidInputs(Materials.SoulInfusedMedium.getFluid(100))
                    .requiresCleanRoom()
                    .duration(40 * SECONDS)
                    .eut(TierEU.RECIPE_HV),
                asmdTarget);
    }

    private static void removeOldEliteSolderRecipes(RecipeContext recipes, ItemStack[] inputs, int duration) {
        recipes.machine(circuitAssemblerRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(inputs)
                    .itemOutputs(ItemList.Circuit_Elite.get(1))
                    .fluidInputsAnyOf(
                        new FluidStack[] { Materials.Lead.getMolten(1_152) },
                        new FluidStack[] { Materials.SolderingAlloy.getMolten(288) },
                        new FluidStack[] { Materials.Tin.getMolten(576) })
                    .fluidOutputs()
                    .duration(duration)
                    .eut(TierEU.RECIPE_HV)
                    .build(),
                1);
    }

    private static void modifyPpicWafer(RecipeContext recipes) {
        replaceBothChemicalMaps(
            recipes,
            RecipeMatch.builder()
                .itemInputs(
                    ItemList.Circuit_Wafer_NPIC.get(1),
                    GTOreDictUnificator.get(OrePrefixes.dust, Materials.IndiumGalliumPhosphide, 64))
                .itemOutputs(ItemList.Circuit_Wafer_PPIC.get(1))
                .fluidInputs(Materials.Sunnarium.getMolten(1_440))
                .fluidOutputs()
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_ZPM)
                .build(),
            RecipeChange.builder()
                .replaceItemInput(ItemList.Circuit_Wafer_NPIC.get(1), ItemList.Circuit_Wafer_NPIC.get(64))
                .build());
    }

    private static void removeDiodeRecipes(RecipeContext recipes) {
        ItemStack copperWire = GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4);
        ItemStack galliumArsenide = GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.GalliumArsenide, 1);
        ItemStack siliconWafer = ItemList.Circuit_Silicon_Wafer.get(1);
        FluidStack gregTechGlass = Materials.Glass.getMolten(288);
        FluidStack registryGlass = FluidRegistry.getFluidStack("glass.molten", 1_000);

        removeDiodeRecipe(recipes, copperWire, galliumArsenide, 2, gregTechGlass);
        removeDiodeRecipe(recipes, copperWire, galliumArsenide, 2, registryGlass);
        removeDiodeRecipe(recipes, copperWire, siliconWafer, 1, gregTechGlass);
        removeDiodeRecipe(recipes, copperWire, siliconWafer, 1, registryGlass);
    }

    private static void removeDiodeRecipe(RecipeContext recipes, ItemStack copperWire, ItemStack semiconductor,
        int outputAmount, FluidStack glass) {
        recipes.machine(assemblerRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(copperWire, semiconductor, GTUtility.getIntegratedCircuit(4))
                    .itemOutputs(ItemList.Circuit_Parts_Diode.get(outputAmount))
                    .fluidInputs(glass)
                    .fluidOutputs()
                    .duration(30 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                1);
    }

    private static void modifyDiamondSawblade(RecipeContext recipes) {
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.gear, Materials.CobaltBrass, 1),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Diamond, 1))
                    .itemOutputs(ItemList.Component_Sawblade_Diamond.get(1))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(16 * SECONDS)
                    .eut(20)
                    .build(),
                RecipeChange.builder()
                    .replaceItemInput(
                        GTOreDictUnificator.get(OrePrefixes.gear, Materials.CobaltBrass, 1),
                        MaterialsAlloy.BLOODSTEEL.getGear(1))
                    .build());
    }

    private static void modifyMachineCasings(RecipeContext recipes) {
        modifyMachineCasing(recipes, Materials.CastIron, ItemList.Casing_ULV.get(1), 1 * SECONDS + 5 * TICKS);
        modifyMachineCasing(recipes, Materials.Steel, ItemList.Casing_LV.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.Aluminium, ItemList.Casing_MV.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.StainlessSteel, ItemList.Casing_HV.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.Titanium, ItemList.Casing_EV.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.TungstenSteel, ItemList.Casing_IV.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(
            recipes,
            WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial(),
            ItemList.Casing_LuV.get(1),
            2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.Iridium, ItemList.Casing_ZPM.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.Osmium, ItemList.Casing_UV.get(1), 2 * SECONDS + 10 * TICKS);
        modifyMachineCasing(recipes, Materials.Neutronium, ItemList.Casing_MAX.get(1), 2 * SECONDS + 10 * TICKS);
    }

    private static void modifyMachineCasing(RecipeContext recipes, Materials material, ItemStack casing, int duration) {
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.plate, material, 8),
                        GTUtility.getIntegratedCircuit(8))
                    .itemOutputs(casing)
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(duration)
                    .eut(TierEU.RECIPE_LV / 2)
                    .build(),
                RecipeChange.builder()
                    .replaceItemInput(
                        GTOreDictUnificator.get(OrePrefixes.plate, material, 8),
                        GTOreDictUnificator.get(OrePrefixes.plateDouble, material, 8))
                    .replaceItemOutput(casing, RawMachineCasings.productionOutput(casing))
                    .build());
    }

    private static void modifyRawNeutronium(RecipeContext recipes) {
        for (RecipeMap<?> map : new RecipeMap<?>[] { mixerRecipes, mixerNonCellRecipes }) {
            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Neutronium, 1),
                            GTUtility.getIntegratedCircuit(1))
                        .itemOutputs(NHItemList.RawNeutronium.get())
                        .fluidInputs(Materials.Helium.getPlasma(144))
                        .fluidOutputs()
                        .duration(11 * SECONDS + 5 * TICKS)
                        .eut(TierEU.RECIPE_ZPM)
                        .build(),
                    RecipeChange.builder()
                        .fluidInputAmount(Materials.Helium.getPlasma(144), 1_440)
                        .build());
        }
    }

    private static void modifyIndiumCombRecipes(RecipeContext recipes) {
        if (!Forestry.isModLoaded()) {
            return;
        }

        recipes.machine(multiblockChemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 36),
                        GTBees.combs.getStackForType(CombType.INDIUM, 4),
                        GTUtility.getIntegratedCircuit(3))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 3))
                    .fluidInputs(new FluidStack(ItemList.sIndiumConcentrate, 72_000))
                    .fluidOutputs(new FluidStack(ItemList.sLeadZincSolution, 72_000))
                    .duration(22 * SECONDS + 10 * TICKS)
                    .eut(TierEU.RECIPE_HV)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 3), 2)
                    .build());

        recipes.machine(multiblockChemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GGMaterial.indiumPhosphate.get(OrePrefixes.dust, 12),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcium, 3),
                        GTBees.combs.getStackForType(CombType.INDIUM, 8),
                        GTUtility.getIntegratedCircuit(2))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 6),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.TricalciumPhosphate, 5))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(1 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 6), 4)
                    .build());

        RecipeMatch purifiedIndium = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Indium, 4),
                GTBees.combs.getStackForType(CombType.INDIUM, 16))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Indium, 12))
            .fluidInputs(Materials.PhthalicAcid.getFluid(2_688))
            .fluidOutputs()
            .duration(14 * SECONDS + 8 * TICKS)
            .eut(TierEU.RECIPE_IV)
            .build();
        replaceBothChemicalMaps(
            recipes,
            purifiedIndium,
            RecipeChange.builder()
                .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Indium, 12), 8)
                .build());

        recipes.machine(multiblockChemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        WerkstoffLoader.Roquesit.get(OrePrefixes.dust, 4),
                        GTBees.combs.getStackForType(CombType.INDIUM, 4))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 1),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 3),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Sulfur, 2))
                    .fluidInputs(Materials.PhthalicAcid.getFluid(90))
                    .fluidOutputs()
                    .duration(3 * SECONDS + 4 * TICKS)
                    .eut(TierEU.RECIPE_HV)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 3), 2)
                    .build());
    }

    private static void modifyPraseodymiumCentrifuge(RecipeContext recipes) {
        if (!Forestry.isModLoaded()) {
            return;
        }
        for (RecipeMap<?> map : new RecipeMap<?>[] { centrifugeRecipes, centrifugeNonCellRecipes }) {
            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(GTBees.combs.getStackForType(CombType.INDIUM, 8), GTUtility.getIntegratedCircuit(2))
                        .itemOutputs(
                            WerkstoffLoader.Thorium232.get(OrePrefixes.dust, 64),
                            WerkstoffLoader.Thorium232.get(OrePrefixes.dust, 16),
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Praseodymium, 64),
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Praseodymium, 32),
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Boron, 2),
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 12))
                        .fluidInputs(GGMaterial.thoriumBasedLiquidFuelDepleted.getFluidOrGas(1_000))
                        .fluidOutputs()
                        .duration(1 * MINUTES + 15 * SECONDS)
                        .eut(TierEU.RECIPE_EV / 2)
                        .outputChances(10000, 8000, 10000, 8000, 3000, 5000)
                        .build(),
                    RecipeChange.builder()
                        .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 12), 8)
                        .build());
        }
    }

    private static void modifyVacuumFurnaceIndium(RecipeContext recipes) {
        if (!Forestry.isModLoaded()) {
            return;
        }

        recipes.machine(vacuumFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTBees.combs.getStackForType(CombType.INDIUM, 64),
                        GTBees.combs.getStackForType(CombType.INDIUM, 64),
                        GTBees.combs.getStackForType(CombType.INDIUM, 64),
                        GTBees.combs.getStackForType(CombType.INDIUM, 64),
                        GTUtility.getIntegratedCircuit(2))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Zinc, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Zinc, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Zinc, 52),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 56),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 64),
                        MaterialsElements.getInstance().GERMANIUM.getDust(15))
                    .fluidInputs(new FluidStack(GTPPFluids.SphaleriteFlotationFroth, 4_000))
                    .fluidOutputs(new FluidStack(GTPPFluids.RedMud, 2_000), Materials.Water.getFluid(2_000))
                    .duration(2 * MINUTES)
                    .eut(TierEU.RECIPE_LuV)
                    .specialValue(5500)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmounts(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 64), 3, 32)
                    .build());

        recipes.machine(vacuumFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(GTBees.combs.getStackForType(CombType.INDIUM, 40), GTUtility.getIntegratedCircuit(2))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Copper, 52),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 56),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Cadmium, 50),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 30))
                    .fluidInputs(new FluidStack(GTPPFluids.ChalcopyriteFlotationFroth, 4_000))
                    .fluidOutputs(new FluidStack(GTPPFluids.RedMud, 2_000), Materials.Water.getFluid(2_000))
                    .duration(2 * MINUTES)
                    .eut(TierEU.RECIPE_IV)
                    .specialValue(4500)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Indium, 30), 20)
                    .build());
    }

    private static void modifyAdvancedCircuitInputs(RecipeContext recipes) {
        ItemStack output = GTModHandler.getIC2Item("advancedCircuit", 1);
        ItemStack[] standardInputs = { ItemList.Circuit_Integrated_Good.get(1), ItemList.Circuit_Chip_ILC.get(2),
            ItemList.Circuit_Chip_Ram.get(2), ItemList.Circuit_Parts_Transistor.get(4),
            GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Electrum, 8),
            GTOreDictUnificator.get(OrePrefixes.bolt, Materials.AnnealedCopper, 8) };
        ItemStack[] asmdInputs = { ItemList.Circuit_Integrated_Good.get(1), ItemList.Circuit_Chip_ILC.get(2),
            ItemList.Circuit_Chip_Ram.get(2), ItemList.Circuit_Parts_TransistorASMD.get(1),
            GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Electrum, 8),
            GTOreDictUnificator.get(OrePrefixes.bolt, Materials.AnnealedCopper, 8) };

        recipes.machine(circuitAssemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(standardInputs)
                    .itemOutputs(output)
                    .fluidInputCount(1)
                    .fluidOutputs()
                    .duration(40 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .itemInputAmount(ItemList.Circuit_Chip_ILC.get(2), 8)
                    .itemInputAmount(ItemList.Circuit_Chip_Ram.get(2), 8)
                    .build());
        recipes.machine(circuitAssemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(asmdInputs)
                    .itemOutputs(output)
                    .fluidInputCount(1)
                    .fluidOutputs()
                    .duration(20 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .itemInputAmount(ItemList.Circuit_Chip_ILC.get(2), 8)
                    .itemInputAmount(ItemList.Circuit_Chip_Ram.get(2), 8)
                    .build());
    }

    private static void modifyIntegratedLogicCircuitInputs(RecipeContext recipes) {
        ItemStack[] inputs = { ItemList.Circuit_Board_Coated_Basic.get(1), ItemList.Circuit_Chip_ILC.get(1),
            ItemList.Circuit_Parts_Resistor.get(2), ItemList.Circuit_Parts_Diode.get(2),
            GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 2),
            GTOreDictUnificator.get(OrePrefixes.bolt, Materials.Tin, 2) };
        recipes.machine(circuitAssemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(inputs)
                    .itemOutputs(ItemList.Circuit_Basic.get(1))
                    .fluidInputCount(1)
                    .fluidOutputs()
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_LV / 2)
                    .build(),
                RecipeChange.builder()
                    .itemInputAmount(ItemList.Circuit_Chip_ILC.get(1), 4)
                    .build());
    }

    private static void modifyGoodCircuitInputs(RecipeContext recipes) {
        ItemStack[] inputs = { ItemList.Circuit_Board_Phenolic_Good.get(1),
            getModItem(Mods.IndustrialCraft2.ID, "itemPartCircuit", 2, 0), ItemList.Circuit_Parts_Diode.get(2),
            GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.Copper, 2) };
        recipes.machine(circuitAssemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(inputs)
                    .itemOutputs(ItemList.Circuit_Good.get(1))
                    .fluidInputCount(1)
                    .fluidOutputs()
                    .duration(15 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .itemInputAmount(getModItem(Mods.IndustrialCraft2.ID, "itemPartCircuit", 2, 0), 4)
                    .build());
    }

    private static void modifyCupronickelCoilInputs(RecipeContext recipes) {
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Cupronickel, 8),
                        NHItemList.AluminoSilicateWool.get(12),
                        GTUtility.getIntegratedCircuit(3))
                    .itemOutputs(ItemList.Casing_Coil_Cupronickel.get(1))
                    .fluidInputs(Materials.Tin.getMolten(288))
                    .fluidOutputs()
                    .duration(5 * SECONDS)
                    .eut(7)
                    .build(),
                RecipeChange.builder()
                    .replaceItemInput(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Cupronickel, 8),
                        GTOreDictUnificator.get(OrePrefixes.wireGt08, Materials.Cupronickel, 8))
                    .itemInputAmount(NHItemList.AluminoSilicateWool.get(12), 24)
                    .build());
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Cupronickel, 8),
                        NHItemList.MicaInsulatorFoil.get(8),
                        GTUtility.getIntegratedCircuit(3))
                    .itemOutputs(ItemList.Casing_Coil_Cupronickel.get(1))
                    .fluidInputs(Materials.Tin.getMolten(144))
                    .fluidOutputs()
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .replaceItemInput(
                        GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Cupronickel, 8),
                        GTOreDictUnificator.get(OrePrefixes.wireGt08, Materials.Cupronickel, 8))
                    .itemInputAmount(NHItemList.MicaInsulatorFoil.get(8), 16)
                    .build());
    }

    private static void modifyMvEnergyHatchInputs(RecipeContext recipes) {
        ItemStack[] inputs = { GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 1),
            ItemList.Electric_Pump_MV.get(1), ItemList.Hull_MV.get(1), ItemList.MV_Coil.get(2),
            ItemList.Circuit_Chip_ULPIC.get(2), GTUtility.getIntegratedCircuit(4) };
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(inputs)
                    .itemOutputs(ItemList.Hatch_Energy_MV.get(1))
                    .fluidInputs(Materials.Lubricant.getFluid(2_000))
                    .fluidOutputs()
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .build(),
                RecipeChange.builder()
                    .itemInputAmount(ItemList.Circuit_Chip_ULPIC.get(2), 4)
                    .build());
    }

    private static void replaceBothChemicalMaps(RecipeContext recipes, RecipeMatch oldRecipe, RecipeChange mutation) {
        replaceOne(recipes, chemicalReactorRecipes, oldRecipe, mutation);
        replaceOne(recipes, multiblockChemicalReactorRecipes, oldRecipe, mutation);
    }

    private static void replaceOne(RecipeContext recipes, RecipeMap<?> map, RecipeMatch oldRecipe,
        RecipeChange mutation) {
        recipes.machine(map)
            .modifyOne(oldRecipe, mutation);
    }
}
