package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.dreammaster.scripts.IngredientFactory;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Checks the recipes players can actually find in the EBF and Alloy Blast Smelter. */
final class AlloyBlastSmelterIntegrationTests {

    private AlloyBlastSmelterIntegrationTests() {}

    static void verify() {
        verifySteelAndAluminium();
    }

    private static void verifySteelAndAluminium() {
        RecipeMap<?> smelter = RecipeMaps.alloyBlastSmelterRecipes;
        check(
            find(
                smelter,
                new ItemStack[] { Materials.Iron.getDust(1), Materials.Coal.getDust(1),
                    GTUtility.getIntegratedCircuit(11) },
                Materials.Oxygen.getGas(1_000)) == null,
            "Alloy Blast Smelter still turns iron and carbon directly into steel");
        check(
            find(
                smelter,
                new ItemStack[] { Materials.CastIron.getDust(1), GTUtility.getIntegratedCircuit(11) },
                Materials.Oxygen.getGas(1_000)) == null,
            "Alloy Blast Smelter still accepts the old cast iron cost");
        ItemStack[] castIron = { Materials.CastIron.getDust(3), GTUtility.getIntegratedCircuit(11) };
        equivalentSmelting("cast iron", castIron, Materials.Steel.getMolten(432), Materials.Oxygen.getGas(6_000));

        verifyAluminium(Materials.Aluminium.getDust(1));
        verifyAluminium(IngredientFactory.getModItem("TConstruct", "materials", 1, 12));
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
