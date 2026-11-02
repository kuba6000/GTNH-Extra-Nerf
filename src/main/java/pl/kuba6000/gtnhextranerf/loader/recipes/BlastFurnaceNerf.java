package pl.kuba6000.gtnhextranerf.loader.recipes;

import static com.dreammaster.scripts.IngredientFactory.getModItem;
import static gregtech.api.recipe.RecipeMaps.alloyBlastSmelterRecipes;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.recipe.RecipeMaps.primitiveBlastRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

public final class BlastFurnaceNerf implements RecipeModule {

    private static final int EXPECTED_ALLOWED_PRIMITIVE_STEEL_RECIPES = 7;
    private static final int EXPECTED_REMOVED_PRIMITIVE_STEEL_RECIPES = 39;

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(
            AuditScope.RESTRICT_PRIMITIVE_BLAST_FURNACE_STEEL_FUELS,
            BlastFurnaceNerf::restrictPrimitiveBlastFurnaceFuels);
        registry.register(AuditScope.MODIFY_STEEL_BLAST_FURNACE_RECIPES, BlastFurnaceNerf::modifySteelEBF);
        registry.register(AuditScope.MODIFY_ALUMINIUM_BLAST_FURNACE_RECIPES, BlastFurnaceNerf::modifyAluminiumEBF);
        registry.register(
            AuditScope.MODIFY_TINKERS_ALUMINIUM_BLAST_FURNACE_RECIPES,
            BlastFurnaceNerf::modifyTinkersAluminiumEBF);
        registry.register(AuditScope.MODIFY_ILMENITE_BLAST_FURNACE_RECIPE, BlastFurnaceNerf::modifyIlmeniteEBF);
        registry.register(AuditScope.MODIFY_TITANIUM_BLAST_FURNACE_RECIPE, BlastFurnaceNerf::modifyTitaniumEBF);
    }

    private static void restrictPrimitiveBlastFurnaceFuels(RecipeContext recipes) {
        ItemStack coalCokeBlock = getModItem("Railcraft", "cube", 1, 0);
        if (!GTUtility.isStackValid(coalCokeBlock)) {
            throw new RecipeValidationException("Could not resolve the Railcraft Coal Coke Block");
        }
        // Full-pack steel baseline: unrelated primitive blast recipes remain untouched.
        recipes.machine(primitiveBlastRecipes)
            .retainExactly(
                BlastFurnaceNerf::producesSteel,
                recipe -> containsAllowedPrimitiveBlastFuel(recipe, coalCokeBlock),
                EXPECTED_ALLOWED_PRIMITIVE_STEEL_RECIPES,
                EXPECTED_REMOVED_PRIMITIVE_STEEL_RECIPES);
    }

    private static boolean producesSteel(GTRecipe recipe) {
        if (recipe.mOutputs == null) return false;
        for (ItemStack output : recipe.mOutputs) {
            if (GTOreDictUnificator.isItemStackInstanceOf(output, "ingotSteel")) return true;
        }
        return false;
    }

    private static boolean containsAllowedPrimitiveBlastFuel(GTRecipe recipe, ItemStack coalCokeBlock) {
        for (ItemStack input : recipe.mInputs) {
            if (GTOreDictUnificator.isItemStackInstanceOf(input, "fuelCoke")
                || GTUtility.areStacksEqual(input, coalCokeBlock, false)) {
                return true;
            }
        }
        return false;
    }

    private static void modifySteelEBF(RecipeContext recipes) {
        recipes.machine(blastFurnaceRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1),
                        GTUtility.getIntegratedCircuit(11))
                    .oreDictInput(OrePrefixes.dust.name() + Materials.AnyCarbon.mName, 1)
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 1),
                        Materials.Ash.getDust(1))
                    .fluidInputs(Materials.Oxygen.getGas(1_000))
                    .fluidOutputs(Materials.CarbonDioxide.getGas(250))
                    .duration(25 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .specialValue(1000)
                    .build(),
                1);

        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.CastIron, 1),
                        GTUtility.getIntegratedCircuit(11))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 1),
                        Materials.Ash.getDust(1))
                    .fluidInputs(Materials.Oxygen.getGas(1_000))
                    .fluidOutputs()
                    .duration(5 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .specialValue(1000)
                    .build(),
                RecipeChange.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.CastIron, 3),
                        GTUtility.getIntegratedCircuit(11))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 3),
                        Materials.Ash.getDust(1))
                    .fluidInputs(Materials.Oxygen.getGas(6_000))
                    .fluidOutputs(Materials.CarbonDioxide.getGas(2_500))
                    .duration(45 * SECONDS)
                    .build());

        recipes.machine(alloyBlastSmelterRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        Materials.Iron.getDust(1),
                        Materials.Coal.getDust(1),
                        GTUtility.getIntegratedCircuit(11))
                    .itemOutputs()
                    .fluidInputs(Materials.Oxygen.getGas(1_000))
                    .fluidOutputs(Materials.Steel.getMolten(144))
                    .duration(20 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .build(),
                1);
        recipes.machine(alloyBlastSmelterRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(Materials.CastIron.getDust(1), GTUtility.getIntegratedCircuit(11))
                    .itemOutputs()
                    .fluidInputs(Materials.Oxygen.getGas(1_000))
                    .fluidOutputs(Materials.Steel.getMolten(144))
                    .duration(4 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .build(),
                RecipeChange.builder()
                    .itemInputAmount(Materials.CastIron.getDust(1), 3)
                    .fluidInputAmount(Materials.Oxygen.getGas(1_000), 6_000)
                    .fluidOutputAmount(Materials.Steel.getMolten(144), 432)
                    .duration(36 * SECONDS)
                    .build());

        RecipeMatch ironToCastIron = RecipeMatch.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 3), GTUtility.getIntegratedCircuit(1))
            .oreDictInput(OrePrefixes.dust.name() + Materials.AnyCarbon.mName, 2)
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.CastIron, 3))
            .fluidInputs()
            .fluidOutputs()
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .specialValue(1000)
            .build();
        recipes.machine(blastFurnaceRecipes)
            .removeExactly(ironToCastIron, 0);
        recipes.machine(blastFurnaceRecipes)
            .addOne(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 3),
                        GTUtility.getIntegratedCircuit(1),
                        new OreDictItemStack(OrePrefixes.dust.name() + Materials.AnyCarbon.mName, 2))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.CastIron, 3))
                    .duration(20 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .metadata(COIL_HEAT, 1000),
                ironToCastIron);

        assertEvSteelRecipeUnchanged(recipes);
    }

    private static void assertEvSteelRecipeUnchanged(RecipeContext recipes) {
        recipes.machine(blastFurnaceRecipes)
            .requireExactlyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 50),
                        GTUtility.getIntegratedCircuit(2))
                    .oreDictInput(OrePrefixes.dust.name() + Materials.AnyCarbon.mName, 1)
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 50),
                        Materials.Ash.getDust(1))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(150 * SECONDS)
                    .eut(TierEU.RECIPE_EV)
                    .specialValue(2000)
                    .build());
    }

    private static void modifyAluminiumEBF(RecipeContext recipes) {
        ItemStack aluminiumDust = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 1);
        ItemStack aluminiumIngot = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Aluminium, 1);

        recipes.machine(blastFurnaceRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminiumoxide, 10),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 3))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Aluminium, 4))
                    .fluidInputs()
                    .fluidOutputs(Materials.CarbonDioxide.getGas(3_000))
                    .duration(60 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .specialValue(2054)
                    .build(),
                1);

        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminiumoxide, 10),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Cryolite, 5))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Aluminium, 4))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(80 * SECONDS)
                    .eut(TierEU.RECIPE_MV)
                    .specialValue(963)
                    .build(),
                RecipeChange.builder()
                    .duration(2 * MINUTES)
                    .build());

        replaceGasDuration(
            recipes,
            aluminiumDust,
            aluminiumIngot,
            Materials.Nitrogen.getGas(1_000),
            60 * SECONDS,
            90 * SECONDS,
            TierEU.RECIPE_HV,
            2054);
        replaceNoGasRecipe(
            recipes,
            aluminiumDust,
            aluminiumIngot,
            75 * SECONDS,
            2 * MINUTES + 20 * SECONDS,
            TierEU.RECIPE_HV,
            2054,
            2054);
        removeObsoleteGasVariants(recipes, aluminiumDust, aluminiumIngot, TierEU.RECIPE_HV, 2054);
        modifyAlloyBlastSmelterAluminium(recipes, aluminiumDust, TierEU.RECIPE_HV, 140 * SECONDS);
    }

    private static void modifyIlmeniteEBF(RecipeContext recipes) {
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Ilmenite, 5),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.WroughtIron, 1),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Rutile, 3))
                    .fluidInputs()
                    .fluidOutputs(Materials.CarbonMonoxide.getGas(1_000))
                    .duration(2 * MINUTES + 40 * SECONDS)
                    .eut(TierEU.RECIPE_HV)
                    .specialValue(1700)
                    .build(),
                RecipeChange.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Ilmenite, 10),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 2))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.WroughtIron, 2),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Rutile, 6),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.IlmeniteSlag, 1))
                    .fluidOutputs(Materials.CarbonMonoxide.getGas(2_000))
                    .duration(5 * MINUTES)
                    .build());
    }

    private static void modifyTitaniumEBF(RecipeContext recipes) {
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Magnesium, 2),
                        GTUtility.getIntegratedCircuit(12))
                    .itemOutputs(
                        GTOreDictUnificator.get(OrePrefixes.ingotHot, Materials.Titanium, 1),
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Magnesiumchloride, 6))
                    .fluidInputs(Materials.Titaniumtetrachloride.getFluid(1_000))
                    .fluidOutputs()
                    .duration(40 * SECONDS)
                    .eut(TierEU.RECIPE_HV)
                    .specialValue(2140)
                    .build(),
                RecipeChange.builder()
                    .itemOutputAmount(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Magnesiumchloride, 6), 4)
                    .build());
    }

    @SuppressWarnings("deprecation")
    private static void modifyTinkersAluminiumEBF(RecipeContext recipes) {
        ItemStack tinkersAluminium = getModItem("TConstruct", "materials", 1, 12);
        ItemStack aluminiumIngot = Materials.Aluminium.getIngots(1);

        replaceGasDuration(
            recipes,
            tinkersAluminium,
            aluminiumIngot,
            Materials.Nitrogen.getGas(1_000),
            60 * SECONDS,
            90 * SECONDS,
            TierEU.RECIPE_MV,
            1300);
        replaceNoGasRecipe(
            recipes,
            tinkersAluminium,
            aluminiumIngot,
            75 * SECONDS,
            2 * MINUTES,
            TierEU.RECIPE_MV,
            1300,
            1600);
        removeObsoleteGasVariants(recipes, tinkersAluminium, aluminiumIngot, TierEU.RECIPE_MV, 1300);
        modifyAlloyBlastSmelterAluminium(recipes, tinkersAluminium, TierEU.RECIPE_MV, 120 * SECONDS);
    }

    private static void modifyAlloyBlastSmelterAluminium(RecipeContext recipes, ItemStack input, long eut,
        int noGasDuration) {
        recipes.machine(alloyBlastSmelterRecipes)
            .modifyOne(
                alloySmelterAluminium(input, GTUtility.getIntegratedCircuit(1), null, 75 * SECONDS, eut),
                RecipeChange.builder()
                    .duration(smelterDuration(noGasDuration))
                    .build());
        recipes.machine(alloyBlastSmelterRecipes)
            .modifyOne(
                alloySmelterAluminium(
                    input,
                    GTUtility.getIntegratedCircuit(11),
                    Materials.Nitrogen.getGas(1_000),
                    60 * SECONDS,
                    eut),
                RecipeChange.builder()
                    .duration(smelterDuration(90 * SECONDS))
                    .build());
        RecipeMatch obsolete = RecipeMatch.anyOf(
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                Materials.Helium.getGas(1_000),
                54 * SECONDS,
                eut),
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                Materials.Argon.getGas(850),
                48 * SECONDS,
                eut),
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                Materials.Radon.getGas(700),
                42 * SECONDS,
                eut),
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                WerkstoffLoader.Neon.getFluidOrGas(550),
                36 * SECONDS,
                eut),
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                WerkstoffLoader.Krypton.getFluidOrGas(400),
                30 * SECONDS,
                eut),
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                WerkstoffLoader.Xenon.getFluidOrGas(250),
                24 * SECONDS,
                eut),
            alloySmelterAluminium(
                input,
                GTUtility.getIntegratedCircuit(11),
                WerkstoffLoader.Oganesson.getFluidOrGas(100),
                18 * SECONDS,
                eut));
        recipes.machine(alloyBlastSmelterRecipes)
            .removeExactly(obsolete, 7);
    }

    private static RecipeMatch alloySmelterAluminium(ItemStack input, ItemStack circuit, FluidStack gas,
        int ebfDuration, long eut) {
        RecipeMatch.Builder match = RecipeMatch.builder()
            .itemInputs(input, circuit)
            .itemOutputs()
            .fluidOutputs(Materials.Aluminium.getMolten(144))
            .duration(smelterDuration(ebfDuration))
            .eut(eut);
        if (gas == null) match.fluidInputs();
        else match.fluidInputs(gas);
        return match.build();
    }

    private static int smelterDuration(int ebfDuration) {
        // GT++ generates Alloy Blast Smelter recipes at 80% of the EBF duration.
        return ebfDuration * 4 / 5;
    }

    private static RecipeMatch gasRecipe(ItemStack input, ItemStack output, FluidStack gas, int duration, long eut,
        int heat) {
        return RecipeMatch.builder()
            .itemInputs(input, GTUtility.getIntegratedCircuit(11))
            .itemOutputs(output)
            .fluidInputs(gas)
            .fluidOutputs()
            .duration(duration)
            .eut(eut)
            .specialValue(heat)
            .build();
    }

    private static void replaceGasDuration(RecipeContext recipes, ItemStack input, ItemStack output, FluidStack gas,
        int oldDuration, int newDuration, long eut, int heat) {
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                gasRecipe(input, output, gas, oldDuration, eut, heat),
                RecipeChange.builder()
                    .duration(newDuration)
                    .build());
    }

    private static void replaceNoGasRecipe(RecipeContext recipes, ItemStack input, ItemStack output, int oldDuration,
        int newDuration, long eut, int oldHeat, int newHeat) {
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(input, GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(output)
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(oldDuration)
                    .eut(eut)
                    .specialValue(oldHeat)
                    .build(),
                RecipeChange.builder()
                    .duration(newDuration)
                    .specialValue(newHeat)
                    .build());
    }

    private static void removeObsoleteGasVariants(RecipeContext recipes, ItemStack input, ItemStack output, long eut,
        int heat) {
        RecipeMatch obsolete = RecipeMatch.anyOf(
            gasRecipe(input, output, Materials.Helium.getGas(1_000), 54 * SECONDS, eut, heat),
            gasRecipe(input, output, Materials.Argon.getGas(850), 48 * SECONDS, eut, heat),
            gasRecipe(input, output, Materials.Radon.getGas(700), 42 * SECONDS, eut, heat),
            gasRecipe(input, output, WerkstoffLoader.Neon.getFluidOrGas(550), 36 * SECONDS, eut, heat),
            gasRecipe(input, output, WerkstoffLoader.Krypton.getFluidOrGas(400), 30 * SECONDS, eut, heat),
            gasRecipe(input, output, WerkstoffLoader.Xenon.getFluidOrGas(250), 24 * SECONDS, eut, heat),
            gasRecipe(input, output, WerkstoffLoader.Oganesson.getFluidOrGas(100), 18 * SECONDS, eut, heat));
        recipes.machine(blastFurnaceRecipes)
            .removeExactly(obsolete, 7);
    }
}
