package pl.kuba6000.gtnhextranerf.loader.recipes;

import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalBathRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.distilleryRecipes;
import static gregtech.api.recipe.RecipeMaps.electrolyzerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.electrolyzerRecipes;
import static gregtech.api.recipe.RecipeMaps.fusionRecipes;
import static gregtech.api.recipe.RecipeMaps.hammerRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeBuilder.TICKS;
import static gregtech.api.util.GTRecipeConstants.FUSION_THRESHOLD;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import bartworks.system.material.WerkstoffLoader;
import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;
import gtnhlanth.common.register.WerkstoffMaterialPool;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingDefinition;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeBuilder;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeType;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

public final class ChemicalAndMiscNerf implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.REDUCE_TPV_MIXER_OUTPUT, ChemicalAndMiscNerf::reduceTpvMixerOutput);
        registry.register(AuditScope.MODIFY_INDIUM_CONCENTRATE_RECIPES, ChemicalAndMiscNerf::modifyIndiumConcentrate);
        registry.register(AuditScope.MODIFY_PLUTONIUM_DECAY_RECIPES, ChemicalAndMiscNerf::modifyPlutoniumDecay);
        registry.register(AuditScope.MODIFY_SILICON_LINE_RECIPES, ChemicalAndMiscNerf::modifySiliconLine);
        registry.register(AuditScope.ADD_LIVINGROCK_PLATE, ChemicalAndMiscNerf::addLivingrockPlate);
        registry
            .register(AuditScope.MODIFY_LV_OIL_DISTILLERY_RECIPES, ChemicalAndMiscNerf::modifyLvOilDistilleryRecipes);
        registry.register(AuditScope.MODIFY_ENDSTONE_CENTRIFUGE_RECIPE, ChemicalAndMiscNerf::modifyEndstoneCentrifuge);
        registry
            .register(AuditScope.MODIFY_STABALLOY_CENTRIFUGE_RECIPES, ChemicalAndMiscNerf::modifyStaballoyCentrifuge);
        registry
            .register(AuditScope.MODIFY_EUROPIUM_AMERICIUM_FUSION_RECIPES, ChemicalAndMiscNerf::modifyFusionRecipes);
        registry.register(
            AuditScope.MODIFY_AMERICIUM_PLASMA_FUSION_IGNITION,
            ChemicalAndMiscNerf::modifyAmericiumPlasmaFusion);
        registry.register(AuditScope.MODIFY_QUANTUM_STAR_RECIPE, ChemicalAndMiscNerf::modifyNetherStarRecipe);
        registry.register(AuditScope.MODIFY_TELLURIUM_BLAST_FURNACE_CHANCE, ChemicalAndMiscNerf::modifyTelluriumChance);
        registry.register(AuditScope.MODIFY_PLATINUM_SLUDGE_RECIPES, ChemicalAndMiscNerf::modifyPlatinumSludge);
        registry.register(AuditScope.MODIFY_SAMARIUM_RECIPES, ChemicalAndMiscNerf::modifySamariumRecipes);
        registry.register(
            AuditScope.MODIFY_EXTREME_HEAT_EXCHANGER_HAND_CRAFTING,
            ChemicalAndMiscNerf::modifyExtremeHeatExchanger);
    }

    private static void addLivingrockPlate(RecipeContext recipes) {
        ItemStack livingrock = GTOreDictUnificator.get(OrePrefixes.block, Materials.Livingrock, 1);
        ItemStack plate = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Livingrock, 1);
        FluidStack lubricant = Materials.Lubricant.getFluid(22);
        if (!GTUtility.isStackValid(livingrock) || !GTUtility.isStackValid(plate) || lubricant == null)
            throw new RecipeValidationException("Missing Livingrock, Livingrock Plate, or Lubricant");

        recipes.machine(hammerRecipes)
            .addOne(
                GTValues.RA.stdBuilder()
                    .itemInputs(livingrock)
                    .fluidInputs(lubricant)
                    .itemOutputs(plate)
                    .duration(150 * SECONDS)
                    .eut(30),
                RecipeMatch.builder()
                    .itemInputs(livingrock)
                    .fluidInputs(lubricant)
                    .itemOutputs(plate)
                    .fluidOutputs()
                    .duration(150 * SECONDS)
                    .eut(30)
                    .build());
    }

    private static void reduceTpvMixerOutput(RecipeContext recipes) {
        ItemStack originalOutput = GTOreDictUnificator.get(OrePrefixes.dust, Materials.TPV, 7);
        RecipeMatch original = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Titanium, 3),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Platinum, 3),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Vanadium, 1),
                GTUtility.getIntegratedCircuit(1))
            .itemOutputs(originalOutput)
            .fluidInputs()
            .fluidOutputs()
            .duration(8 * SECONDS + 15 * TICKS)
            .eut(TierEU.RECIPE_EV)
            .build();
        RecipeChange change = RecipeChange.builder()
            .itemOutputAmount(originalOutput, 3)
            .build();
        recipes.machine(mixerRecipes)
            .modifyOne(original, change);
        recipes.machine(mixerNonCellRecipes)
            .modifyOne(original, change);
    }

    private static void modifyIndiumConcentrate(RecipeContext recipes) {
        RecipeMatch oldRecipe = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 4),
                GTUtility.getIntegratedCircuit(1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dustTiny, Materials.Indium, 1))
            .fluidInputs(new FluidStack(ItemList.sIndiumConcentrate, 8_000))
            .fluidOutputs(new FluidStack(ItemList.sLeadZincSolution, 8_000))
            .duration(2 * SECONDS + 10 * TICKS)
            .eut(TierEU.RECIPE_HV)
            .build();
        replaceBothChemicalMaps(
            recipes,
            oldRecipe,
            RecipeChange.builder()
                .itemInputs(
                    GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 8),
                    GTUtility.getIntegratedCircuit(1))
                .build());
    }

    private static void modifyPlutoniumDecay(RecipeContext recipes) {
        recipes.machine(multiblockChemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1),
                        GTUtility.getIntegratedCircuit(8))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 64))
                    .fluidInputs(Materials.Air.getGas(8_000))
                    .fluidOutputs(Materials.Radon.getGas(800))
                    .duration(75 * SECONDS)
                    .eut(TierEU.RECIPE_HV)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 64), 63)
                    .build());

        RecipeMatch heliumRoute = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1),
                GTUtility.getIntegratedCircuit(10))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 63))
            .fluidInputs(Materials.Air.getGas(8_000), Materials.Helium.getGas(800))
            .fluidOutputs(Materials.Radon.getGas(960))
            .duration(75 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .build();
        recipes.machine(multiblockChemicalReactorRecipes)
            .removeExactly(heliumRoute, 0);
        recipes.machine(multiblockChemicalReactorRecipes)
            .addOne(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 64),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1))
                    .circuit(10)
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 63))
                    .fluidInputs(Materials.Air.getGas(8_000), Materials.Helium.getGas(800))
                    .fluidOutputs(Materials.Radon.getGas(960))
                    .duration(75 * SECONDS)
                    .eut(TierEU.RECIPE_HV),
                heliumRoute);
    }

    private static void modifySiliconLine(RecipeContext recipes) {
        modifySodiumSiliconRoute(recipes);
        modifyHydrogenCellSiliconRoute(recipes);
        modifyMultiblockHydrogenSiliconRoute(recipes);
        modifyTrichlorosilaneCellRoute(recipes);
        modifyBartWorksZincSiliconRoute(recipes);
    }

    private static void modifyLvOilDistilleryRecipes(RecipeContext recipes) {
        modifyLvOilDistilleryRecipe(recipes, Materials.OilLight, 3);
        modifyLvOilDistilleryRecipe(recipes, Materials.OilMedium, 6);
        modifyLvOilDistilleryRecipe(recipes, Materials.OilHeavy, 9);
    }

    private static void modifyLvOilDistilleryRecipe(RecipeContext recipes, Materials input, int lubricantAmount) {
        recipes.machine(distilleryRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(GTUtility.getIntegratedCircuit(24))
                    .itemOutputs()
                    .fluidInputs(input.getFluid(12))
                    .fluidOutputs(Materials.Lubricant.getFluid(lubricantAmount))
                    .duration(16 * TICKS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .fluidInputAmount(input.getFluid(12), 24)
                    .build());
    }

    private static void modifySodiumSiliconRoute(RecipeContext recipes) {
        RecipeMatch oldRecipe = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Sodium, 4),
                GTUtility.getIntegratedCircuit(1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 1),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Salt, 8))
            .fluidInputs(Materials.SiliconTetrachloride.getFluid(1_000))
            .fluidOutputs()
            .duration(5 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .build();
        replaceBothChemicalMaps(
            recipes,
            oldRecipe,
            RecipeChange.builder()
                .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Salt, 8), 7)
                .build());
    }

    private static void modifyHydrogenCellSiliconRoute(RecipeContext recipes) {
        recipes.machine(chemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.cell, Materials.Hydrogen, 2),
                        GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 1),
                        ItemList.Cell_Empty.get(2))
                    .fluidInputs(Materials.Trichlorosilane.getFluid(1_000))
                    .fluidOutputs(Materials.HydrochloricAcid.getFluid(3_000))
                    .duration(15 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .fluidOutputAmount(Materials.HydrochloricAcid.getFluid(3_000), 2_000)
                    .build());
    }

    private static void modifyMultiblockHydrogenSiliconRoute(RecipeContext recipes) {
        recipes.machine(multiblockChemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 1))
                    .fluidInputs(Materials.Trichlorosilane.getFluid(1_000), Materials.Hydrogen.getGas(2_000))
                    .fluidOutputs(Materials.HydrochloricAcid.getFluid(3_000))
                    .duration(15 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .fluidOutputAmount(Materials.HydrochloricAcid.getFluid(3_000), 2_000)
                    .build());
    }

    private static void modifyTrichlorosilaneCellRoute(RecipeContext recipes) {
        recipes.machine(chemicalReactorRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.cell, Materials.Trichlorosilane, 1),
                        GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 1),
                        ItemList.Cell_Empty.get(1))
                    .fluidInputs(Materials.Hydrogen.getGas(2_000))
                    .fluidOutputs(Materials.HydrochloricAcid.getFluid(3_000))
                    .duration(15 * SECONDS)
                    .eut(TierEU.RECIPE_LV)
                    .build(),
                RecipeChange.builder()
                    .fluidOutputAmount(Materials.HydrochloricAcid.getFluid(3_000), 2_000)
                    .build());
    }

    private static void modifyBartWorksZincSiliconRoute(RecipeContext recipes) {
        RecipeMatch oldRecipe = RecipeMatch.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Zinc, 2), GTUtility.getIntegratedCircuit(1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 1),
                new ItemStack(WerkstoffLoader.items.get(OrePrefixes.dust), 6, 10052))
            .fluidInputs(Materials.SiliconTetrachloride.getFluid(1_000))
            .fluidOutputs()
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .build();
        replaceBothChemicalMaps(
            recipes,
            oldRecipe,
            RecipeChange.builder()
                .itemOutputAmount(new ItemStack(WerkstoffLoader.items.get(OrePrefixes.dust), 6, 10052), 5)
                .build());
    }

    private static void modifyEndstoneCentrifuge(RecipeContext recipes) {
        for (RecipeMap<?> map : new RecipeMap<?>[] { centrifugeRecipes, centrifugeNonCellRecipes }) {
            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Endstone, 36))
                        .itemOutputs(
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 3),
                            WerkstoffLoader.PTMetallicPowder.get(OrePrefixes.dust, 2),
                            new ItemStack(Blocks.sand, 36))
                        .fluidInputs()
                        .fluidOutputs(Materials.Helium.getGas(4_320))
                        .duration(9 * MINUTES + 36 * SECONDS)
                        .eut(20)
                        .outputChances(3750, 2500, 9000)
                        .build(),
                    RecipeChange.builder()
                        .outputChance(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Tungstate, 3), 1000)
                        .fluidOutputAmount(Materials.Helium.getGas(4_320), 1_500)
                        .build());
        }
    }

    private static void modifyAmericiumPlasmaFusion(RecipeContext recipes) {
        recipes.machine(fusionRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs()
                    .itemOutputs()
                    .fluidInputs(Materials.Plutonium241.getMolten(144), Materials.Hydrogen.getGas(2_000))
                    .fluidOutputs(Materials.Americium.getPlasma(144))
                    .duration(64 * TICKS)
                    .eut(98_304)
                    .build()
                    .and(recipe -> recipe.getMetadataOrDefault(FUSION_THRESHOLD, 0L) == 500_000_000L),
                RecipeChange.builder()
                    .metadata(FUSION_THRESHOLD, 1_000_000_000L)
                    .build());
    }

    private static void modifyFusionRecipes(RecipeContext recipes) {
        recipes.machine(fusionRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs()
                    .itemOutputs()
                    .fluidInputs(Materials.Neodymium.getMolten(16), Materials.Hydrogen.getGas(48))
                    .fluidOutputs(Materials.Europium.getMolten(16))
                    .duration(32 * TICKS)
                    .eut(24_576)
                    .build()
                    .and(recipe -> recipe.getMetadataOrDefault(FUSION_THRESHOLD, 0L) == 150_000_000L),
                RecipeChange.builder()
                    .fluidInputAmount(Materials.Neodymium.getMolten(1), 64)
                    .fluidInputAmount(Materials.Hydrogen.getGas(1), 192)
                    .fluidOutputAmount(Materials.Europium.getMolten(1), 4)
                    .build());
        recipes.machine(fusionRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs()
                    .itemOutputs()
                    .fluidInputs(Materials.Neodymium.getMolten(32), Materials.Promethium.getMolten(16))
                    .fluidOutputs(Materials.Europium.getMolten(32))
                    .duration(8 * TICKS)
                    .eut(24_576)
                    .build()
                    .and(recipe -> recipe.getMetadataOrDefault(FUSION_THRESHOLD, 0L) == 150_000_000L),
                RecipeChange.builder()
                    .fluidInputAmount(Materials.Neodymium.getMolten(1), 16)
                    .fluidInputAmount(Materials.Promethium.getMolten(1), 8)
                    .fluidOutputAmount(Materials.Europium.getMolten(1), 16)
                    .duration(32 * TICKS)
                    .build());
        recipes.machine(fusionRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs()
                    .itemOutputs()
                    .fluidInputs(Materials.Lutetium.getMolten(16), Materials.Chrome.getMolten(16))
                    .fluidOutputs(Materials.Americium.getMolten(16))
                    .duration(96 * TICKS)
                    .eut(49_152)
                    .build()
                    .and(recipe -> recipe.getMetadataOrDefault(FUSION_THRESHOLD, 0L) == 200_000_000L),
                RecipeChange.builder()
                    .fluidInputAmount(Materials.Lutetium.getMolten(1), 64)
                    .fluidInputAmount(Materials.Chrome.getMolten(1), 64)
                    .fluidOutputAmount(Materials.Americium.getMolten(1), 4)
                    .build());
        recipes.machine(fusionRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs()
                    .itemOutputs()
                    .fluidInputs(Materials.Lutetium.getMolten(32), Materials.Promethium.getMolten(16))
                    .fluidOutputs(Materials.Americium.getMolten(32))
                    .duration(24 * TICKS)
                    .eut(38_764)
                    .build()
                    .and(recipe -> recipe.getMetadataOrDefault(FUSION_THRESHOLD, 0L) == 200_000_000L),
                RecipeChange.builder()
                    .fluidInputAmount(Materials.Lutetium.getMolten(1), 16)
                    .fluidInputAmount(Materials.Promethium.getMolten(1), 8)
                    .fluidOutputAmount(Materials.Americium.getMolten(1), 16)
                    .duration(96 * TICKS)
                    .build());
    }

    private static void modifyStaballoyCentrifuge(RecipeContext recipes) {
        RecipeMatch original = RecipeMatch.builder()
            .itemInputs(MaterialsAlloy.STABALLOY.getDust(10))
            .itemOutputs(
                MaterialsElements.getInstance().URANIUM238.getDust(9),
                MaterialsElements.getInstance().TITANIUM.getDust(1))
            .fluidInputs()
            .fluidOutputs()
            .outputChances(10000, 10000)
            .duration(48 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .build();
        RecipeChange change = RecipeChange.builder()
            .eut(TierEU.RECIPE_IV)
            .build();
        recipes.machine(centrifugeRecipes)
            .modifyOne(original, change);
        recipes.machine(centrifugeNonCellRecipes)
            .modifyOne(original, change);
    }

    private static void modifyNetherStarRecipe(RecipeContext recipes) {
        recipes.machine(chemicalBathRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(GTOreDictUnificator.get(OrePrefixes.gem, Materials.NetherStar, 1))
                    .itemOutputs(ItemList.QuantumStar.get(1))
                    .fluidInputs(Materials.Radon.getGas(1_250))
                    .fluidOutputs()
                    .duration(1 * MINUTES + 36 * SECONDS)
                    .eut(384)
                    .build(),
                RecipeChange.builder()
                    .fluidInputAmount(Materials.Radon.getGas(1_250), 5_000)
                    .build());
    }

    private static void modifyTelluriumChance(RecipeContext recipes) {
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Lead, 1),
                        GTUtility.getIntegratedCircuit(17))
                    .itemOutputs(Materials.Lead.getIngots(1), Materials.Tellurium.getNuggets(2))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(4 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .specialValue(722)
                    .build(),
                RecipeChange.builder()
                    .outputChance(Materials.Lead.getIngots(1), 10000)
                    .outputChance(Materials.Tellurium.getNuggets(2), 10)
                    .build());
    }

    private static void modifyPlatinumSludge(RecipeContext recipes) {
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(WerkstoffLoader.PTSaltRefined.get(OrePrefixes.dust), GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(WerkstoffLoader.PTMetallicPowder.get(OrePrefixes.dust))
                    .fluidInputs()
                    .fluidOutputs(Materials.Chlorine.getGas(87))
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .specialValue(900)
                    .build(),
                RecipeChange.builder()
                    .fluidOutputs()
                    .build());

        RecipeMatch rawPowder = RecipeMatch.builder()
            .itemInputs(WerkstoffLoader.PTRawPowder.get(OrePrefixes.dust, 4), Materials.Calcium.getDust(1))
            .itemOutputs(Materials.Platinum.getDust(2), WerkstoffLoader.CalciumChloride.get(OrePrefixes.dust, 3))
            .fluidInputs()
            .fluidOutputs()
            .duration(1 * SECONDS + 10 * TICKS)
            .eut(TierEU.RECIPE_LV)
            .build();
        replaceBothChemicalMaps(
            recipes,
            rawPowder,
            RecipeChange.builder()
                .itemOutputAmount(WerkstoffLoader.CalciumChloride.get(OrePrefixes.dust, 3), 2)
                .build());
    }

    private static void modifySamariumRecipes(RecipeContext recipes) {
        for (RecipeMap<?> map : new RecipeMap<?>[] { centrifugeRecipes, centrifugeNonCellRecipes }) {
            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(WerkstoffMaterialPool.DephosphatedSamariumConcentrate.get(OrePrefixes.dust, 6))
                        .itemOutputs(Materials.Samarium.getDust(1), WerkstoffLoader.Thorianit.get(OrePrefixes.dust, 2))
                        .fluidInputs()
                        .fluidOutputs()
                        .duration(10 * SECONDS)
                        .eut(TierEU.RECIPE_EV)
                        .outputChances(9000, 8000)
                        .build(),
                    RecipeChange.builder()
                        .outputChance(Materials.Samarium.getDust(1), 500)
                        .build());
        }

        for (RecipeMap<?> map : new RecipeMap<?>[] { electrolyzerRecipes, electrolyzerNonCellRecipes }) {
            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(
                            WerkstoffMaterialPool.SamariumChlorideSodiumChlorideBlend.get(OrePrefixes.dust, 6),
                            GTUtility.getIntegratedCircuit(1))
                        .itemOutputs(
                            Materials.Samarium.getDust(1),
                            Materials.Sodium.getDust(1),
                            WerkstoffMaterialPool.RarestEarthResidue.get(OrePrefixes.dustTiny, 2))
                        .fluidInputs()
                        .fluidOutputs(Materials.Chlorine.getGas(4_000))
                        .duration(1 * SECONDS)
                        .eut(TierEU.RECIPE_IV)
                        .build(),
                    RecipeChange.builder()
                        .outputChance(Materials.Samarium.getDust(1), 1000)
                        .outputChance(Materials.Sodium.getDust(1), 10000)
                        .outputChance(WerkstoffMaterialPool.RarestEarthResidue.get(OrePrefixes.dustTiny, 2), 10000)
                        .build());

            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(
                            WerkstoffMaterialPool.SamariumChlorideSodiumChlorideBlend.get(OrePrefixes.dust, 54),
                            GTUtility.getIntegratedCircuit(9))
                        .itemOutputs(
                            Materials.Samarium.getDust(9),
                            Materials.Sodium.getDust(9),
                            WerkstoffMaterialPool.RarestEarthResidue.get(OrePrefixes.dust, 2))
                        .fluidInputs()
                        .fluidOutputs(Materials.Chlorine.getGas(36_000))
                        .duration(2 * SECONDS)
                        .eut(TierEU.RECIPE_LuV)
                        .build(),
                    RecipeChange.builder()
                        .outputChance(Materials.Samarium.getDust(9), 1000)
                        .outputChance(Materials.Sodium.getDust(9), 10000)
                        .outputChance(WerkstoffMaterialPool.RarestEarthResidue.get(OrePrefixes.dust, 2), 10000)
                        .build());
        }
    }

    private static void modifyExtremeHeatExchanger(RecipeContext recipes) {
        ItemStack output = ItemRefer.Extreme_Heat_Exchanger.get(1);
        ItemStack exchanger = GTModHandler.getIC2Item("reactorHeatSwitchDiamond", 1L, 1);
        ItemStack pipe = GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.TungstenSteel, 1);
        ItemStack support = GGMaterial.marCeM200.get(OrePrefixes.plate, 1);
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern("EPE", "PHP", "SPS")
            .ingredient('E', exchanger)
            .ingredient('P', pipe)
            .ingredient('H', ItemList.Hull_IV.get(1))
            .ingredient('S', support)
            .output(output)
            .matchType(CraftingRecipeType.GT_SHAPED)
            .build();
        CraftingDefinition replacement = original.toBuilder()
            .ingredient('P', GTOreDictUnificator.get(OrePrefixes.plate, Materials.Tritanium, 1))
            .ingredient('H', ItemList.Hull_ZPM.get(1))
            .build();
        // The upstream REVERSIBLE recipe already registered item data. GT's addItemData never overwrites it.
        if (GTOreDictUnificator.getItemData(output) == null) {
            throw new RecipeValidationException("Extreme Heat Exchanger is missing its upstream recycling data");
        }
        recipes.crafting()
            .removeOne(original);
        recipes.crafting()
            .addOne(replacement);
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
