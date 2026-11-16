package pl.kuba6000.gtnhextranerf.integration;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.dreammaster.scripts.IngredientFactory;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Checks the recipes players can actually find in the EBF and Alloy Blast Smelter. */
final class AlloyBlastSmelterIntegrationTests {

    private AlloyBlastSmelterIntegrationTests() {}

    static void verify() {
        verifySteel();
        verifyAluminium(Materials.Aluminium.getDust(1));
        verifyAluminium(IngredientFactory.getModItem("TConstruct", "materials", 1, 12));
    }

    static void verifySteel() {
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.blastFurnaceRecipes,
            RecipeMaps.alloyBlastSmelterRecipes }) {
            long steelRecipes = map.getAllRecipes()
                .stream()
                .filter(
                    recipe -> Arrays.stream(recipe.mOutputs)
                        .anyMatch(output -> output != null && output.isItemEqual(Materials.Steel.getIngots(1)))
                        || Arrays.stream(recipe.mFluidOutputs)
                            .anyMatch(output -> output != null && output.isFluidEqual(Materials.Steel.getMolten(1))))
                .peek(
                    recipe -> check(
                        recipe.mEUt == TierEU.RECIPE_UHV && recipe.mDuration == 1,
                        map.unlocalizedName + " still has an early or slow industrial steel route"))
                .count();
            check(steelRecipes == 3, map.unlocalizedName + " must have one steel batch and two remelting routes");
            for (Materials material : new Materials[] { Materials.Steel, Materials.SteelMagnetic }) {
                GTRecipe remelting = find(
                    map,
                    new ItemStack[] { material.getDust(1), GTUtility.getIntegratedCircuit(1) });
                check(remelting != null, map.unlocalizedName + " lost remelting for " + material.mName);
                check(
                    remelting.mEUt == TierEU.RECIPE_UHV && remelting.mDuration == 1,
                    map.unlocalizedName + " remelting must require late power and take one tick");
                check(remelting.mFluidInputs.length == 0, "Steel remelting must not require gas");
                if (map == RecipeMaps.blastFurnaceRecipes) {
                    check(
                        remelting.mOutputs.length == 1 && remelting.mOutputs[0].stackSize == 1
                            && remelting.mOutputs[0].isItemEqual(Materials.Steel.getIngots(1))
                            && remelting.mSpecialValue == 1000
                            && remelting.mFluidOutputs.length == 0,
                        "EBF remelting must preserve its steel output and heat requirement");
                } else {
                    check(
                        remelting.mOutputs.length == 0 && remelting.mFluidOutputs.length == 1
                            && remelting.mFluidOutputs[0].isFluidEqual(Materials.Steel.getMolten(1))
                            && remelting.mFluidOutputs[0].amount == 144,
                        "ABS remelting must output one ingot of molten steel");
                }
            }
        }
        check(
            !OreDictionary.getOres("dustAnyCarbon")
                .isEmpty(),
            "The carbon ore dictionary is empty");
        for (ItemStack carbon : OreDictionary.getOres("dustAnyCarbon")) {
            ItemStack ingredient = carbon.copy();
            ingredient.stackSize = 1;
            ItemStack[] inputs = { Materials.Iron.getDust(50), ingredient, GTUtility.getIntegratedCircuit(2) };
            GTRecipe ebf = find(RecipeMaps.blastFurnaceRecipes, inputs);
            GTRecipe smelter = find(RecipeMaps.alloyBlastSmelterRecipes, inputs);
            check(ebf != null && smelter != null, "A dustAnyCarbon variant lost the late steel batch");
            check(
                ebf.mOutputs[0].stackSize == 50 && ebf.mSpecialValue == 2000,
                "The late EBF steel batch changed its output or heat requirement");
            check(
                ebf.mOutputs.length == 2 && ebf.mOutputs[1].isItemEqual(Materials.Ash.getDust(1))
                    && ebf.getOutputChance(1) == 1111,
                "The late EBF steel batch lost its ash chance");
            check(
                smelter.mFluidOutputs.length == 1 && smelter.mFluidOutputs[0].amount == 7200,
                "The late ABS steel batch has an incorrect output");
        }
    }

    private static void verifyAluminium(ItemStack input) {
        ItemStack[] noGas = { input, GTUtility.getIntegratedCircuit(1) };
        equivalentSmelting("aluminium without gas", noGas, Materials.Aluminium.getMolten(144));
        ItemStack[] withGas = { input, GTUtility.getIntegratedCircuit(11) };
        equivalentSmelting(
            "aluminium with nitrogen",
            withGas,
            Materials.Aluminium.getMolten(144),
            Materials.Nitrogen.getGas(1_000));
        FluidStack[] removed = { Materials.Helium.getGas(1_000), Materials.Argon.getGas(850),
            Materials.Radon.getGas(700), WerkstoffLoader.Neon.getFluidOrGas(550),
            WerkstoffLoader.Krypton.getFluidOrGas(400), WerkstoffLoader.Xenon.getFluidOrGas(250),
            WerkstoffLoader.Oganesson.getFluidOrGas(100) };
        for (FluidStack gas : removed) {
            check(
                find(RecipeMaps.alloyBlastSmelterRecipes, withGas, gas) == null,
                "Alloy Blast Smelter retained a removed aluminium gas route: " + gas.getUnlocalizedName());
        }
    }

    private static void equivalentSmelting(String name, ItemStack[] inputs, FluidStack molten, FluidStack... fluids) {
        GTRecipe ebf = find(RecipeMaps.blastFurnaceRecipes, inputs, fluids);
        GTRecipe smelter = find(RecipeMaps.alloyBlastSmelterRecipes, inputs, fluids);
        check(ebf != null && smelter != null, name + " is missing from an expected machine");
        check(
            smelter.mEUt == ebf.mEUt && smelter.mDuration * 5 == ebf.mDuration * 4,
            name + " has an outdated Alloy Blast Smelter duration or voltage");
        check(
            smelter.mFluidOutputs.length == 1 && smelter.mFluidOutputs[0].isFluidEqual(molten)
                && smelter.mFluidOutputs[0].amount == molten.amount,
            name + " has an incorrect molten output");
    }

    private static GTRecipe find(RecipeMap<?> map, ItemStack[] inputs, FluidStack... fluids) {
        return map.findRecipeQuery()
            .items(inputs)
            .fluids(fluids)
            .find();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
