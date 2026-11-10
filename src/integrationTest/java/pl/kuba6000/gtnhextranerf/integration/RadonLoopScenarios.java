package pl.kuba6000.gtnhextranerf.integration;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

@GameTestHolder("gtnhextranerfradon")
public final class RadonLoopScenarios {

    @GameTest
    public static void smallBatch(GameTestHelper helper) {
        RecipeMatch expected = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 8),
                GTOreDictUnificator.get(OrePrefixes.dustTiny, Materials.Uranium, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 7))
            .fluidInputs(Materials.Air.getGas(1_000))
            .fluidOutputs(Materials.Radon.getGas(100))
            .duration(12_000)
            .eut(TierEU.RECIPE_ULV)
            .build();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.chemicalReactorRecipes,
            RecipeMaps.multiblockChemicalReactorRecipes }) {
            helper.assertTrue(
                map.getAllRecipes()
                    .stream()
                    .filter(expected)
                    .count() == 1,
                "Small radon batch must return seven plutonium in " + map.unlocalizedName);
        }
        helper.succeed();
    }

    @GameTest
    public static void largeBatch(GameTestHelper helper) {
        RecipeMatch expected = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Plutonium, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1),
                GTUtility.getIntegratedCircuit(8))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 63))
            .fluidInputs(Materials.Air.getGas(8_000))
            .fluidOutputs(Materials.Radon.getGas(800))
            .duration(1_500)
            .eut(TierEU.RECIPE_HV)
            .build();
        helper.assertTrue(
            RecipeMaps.multiblockChemicalReactorRecipes.getAllRecipes()
                .stream()
                .filter(expected)
                .count() == 1,
            "Large radon batch must return 63 plutonium");
        helper.succeed();
    }
}
