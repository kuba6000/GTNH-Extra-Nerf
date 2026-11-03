package pl.kuba6000.gtnhextranerf.integration;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.dreammaster.item.NHItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

final class HeatingCoilIntegrationTests {

    private HeatingCoilIntegrationTests() {}

    static void verify() {
        verifyAssembler(Materials.Kanthal, ItemList.Casing_Coil_Kanthal, NHItemList.MicaInsulatorFoil.get(24));
        verifyAssembler(Materials.Nichrome, ItemList.Casing_Coil_Nichrome, NHItemList.MicaInsulatorFoil.get(32));
        verifyAssembler(Materials.TPV, ItemList.Casing_Coil_TungstenSteel, NHItemList.MicaInsulatorFoil.get(40));
        verifyAssembler(Materials.HSSG, ItemList.Casing_Coil_HSSG, NHItemList.MicaInsulatorFoil.get(48));
        verifyAssembler(Materials.HSSS, ItemList.Casing_Coil_HSSS, NHItemList.MicaInsulatorFoil.get(56));
        verifyAssembler(Materials.Naquadah, ItemList.Casing_Coil_Naquadah, NHItemList.MicaInsulatorFoil.get(64));
        verifyAssembler(
            Materials.NaquadahAlloy,
            ItemList.Casing_Coil_NaquadahAlloy,
            NHItemList.MicaInsulatorFoil.get(72));
        verifyAssembler(
            Materials.Trinium,
            ItemList.Casing_Coil_Trinium,
            ItemList.Naquarite_Universal_Insulator_Foil.get(16));
        verifyAssembler(
            Materials.ElectrumFlux,
            ItemList.Casing_Coil_ElectrumFlux,
            ItemList.Naquarite_Universal_Insulator_Foil.get(24));
        verifyAssembler(
            Materials.DraconiumAwakened,
            ItemList.Casing_Coil_AwakenedDraconium,
            ItemList.Naquarite_Universal_Insulator_Foil.get(32));
        verifyAssemblyLine(
            ItemList.Casing_Coil_Infinity,
            GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.Infinity, 8),
            24);
        verifyAssemblyLine(ItemList.Casing_Coil_Hypogen, GTOreDictUnificator.get("wireGt02Hypogen", 8), 32);
        verifyAssemblyLine(
            ItemList.Casing_Coil_Eternal,
            GTOreDictUnificator.get(OrePrefixes.wireGt02, Materials.SpaceTime, 8),
            64);
    }

    private static void verifyAssembler(Materials material, ItemList coil, ItemStack insulation) {
        RecipeMatch expected = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.wireGt08, material, 8),
                insulation,
                GTUtility.getIntegratedCircuit(3))
            .itemOutputs(coil.get(1))
            .build();
        long recipes = RecipeMaps.assemblerRecipes.getAllRecipes()
            .stream()
            .filter(
                recipe -> Arrays.stream(recipe.mOutputs)
                    .anyMatch(coil::isStackEqual))
            .count();
        check(
            recipes == 1 && RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(expected),
            "Unexpected heating coil cost: " + coil);
        if (insulation.stackSize > insulation.getMaxStackSize()) {
            GTRecipe recipe = RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .filter(expected)
                .findFirst()
                .get();
            ItemStack first = insulation.copy();
            first.stackSize = first.getMaxStackSize();
            ItemStack second = insulation.copy();
            second.stackSize -= first.stackSize;
            ItemStack wire = GTOreDictUnificator.get(OrePrefixes.wireGt08, material, 8);
            FluidStack[] fluids = Arrays.stream(recipe.mFluidInputs)
                .map(FluidStack::copy)
                .toArray(FluidStack[]::new);
            check(
                recipe.isRecipeInputEqual(true, fluids, wire, first, second, GTUtility.getIntegratedCircuit(3)),
                "Split coil insulation was rejected: " + coil);
            check(
                first.stackSize == 0 && second.stackSize == 0 && wire.stackSize == 0,
                "Split coil inputs were not consumed: " + coil);
        }
    }

    private static void verifyAssemblyLine(ItemList coil, ItemStack wire, int foilAmount) {
        GTRecipe.RecipeAssemblyLine[] recipes = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> coil.isStackEqual(recipe.mOutput))
            .toArray(GTRecipe.RecipeAssemblyLine[]::new);
        check(recipes.length == 1, "Unexpected Assembly Line coil variants: " + coil);
        check(
            Arrays.stream(recipes[0].mInputs)
                .anyMatch(input -> ItemStack.areItemStacksEqual(input, wire)),
            "Unexpected Assembly Line coil wire: " + coil);
        check(
            Arrays.stream(recipes[0].mInputs)
                .anyMatch(
                    input -> ItemStack
                        .areItemStacksEqual(input, ItemList.Naquarite_Universal_Insulator_Foil.get(foilAmount))),
            "Unexpected Assembly Line coil insulation: " + coil);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
