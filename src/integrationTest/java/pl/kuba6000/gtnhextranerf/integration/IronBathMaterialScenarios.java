package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import pl.kuba6000.gtnhextranerf.materials.IronBathMaterials;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

@GameTestHolder("gtnhextranerftests")
public final class IronBathMaterialScenarios {

    private IronBathMaterialScenarios() {}

    @GameTest(template = "native_fixture")
    public static void bathMetalsRetainIdentityThroughNativeProcessing(GameTestHelper helper) {
        helper.assertTrue(Materials.WroughtIron == IronBathMaterials.wroughtIron(), "GT Wrought Iron identity differs");
        Materials[] metals = { Materials.WroughtIron, Materials.CastIron, Materials.PigIron };
        for (Materials material : metals) {
            helper.assertTrue(material.mMaterialInto == material, material + " is a unification alias");
            helper.assertTrue(material.mSmeltInto == material, material + " loses its identity when smelted");
            helper.assertTrue(material.mMacerateInto == material, material + " loses its identity when macerated");
            helper.assertTrue(material.mArcSmeltInto == material, material + " loses its identity in the arc furnace");
            helper.assertTrue(!material.mOreReRegistrations.contains(Materials.AnyIron), material + " matches AnyIron");
            ItemStack ingot = GTOreDictUnificator.get(OrePrefixes.ingot, material, 1);
            ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dust, material, 1);
            helper.assertTrue(ingot != null && dust != null, material + " is missing ingot or dust");
            for (ItemStack stack : new ItemStack[] { ingot, dust }) {
                ItemData data = GTOreDictUnificator.getItemData(GTOreDictUnificator.get(stack));
                helper.assertTrue(data != null && data.mMaterial.mMaterial == material, material + " was unified away");
            }
            helper.assertTrue(
                RecipeMaps.maceratorRecipes.getAllRecipes()
                    .stream()
                    .filter(
                        RecipeMatch.builder()
                            .itemInputs(ingot)
                            .build())
                    .anyMatch(
                        RecipeMatch.builder()
                            .itemOutputs(dust)
                            .build()),
                material + " ingot cannot be macerated into its own dust");
            helper.assertTrue(
                RecipeMaps.fluidExtractionRecipes.getAllRecipes()
                    .stream()
                    .filter(
                        RecipeMatch.builder()
                            .itemInputs(ingot)
                            .build())
                    .anyMatch(
                        RecipeMatch.builder()
                            .fluidOutputs(material.getMolten(144))
                            .build()),
                material + " ingot cannot be fluid-extracted into its own molten metal");
        }
        helper.succeed();
    }

    @GameTest(template = "native_fixture")
    public static void castIronCannotBeWashedIntoOrdinaryIron(GameTestHelper helper) {
        for (GTRecipe recipe : RecipeMaps.polarizerRecipes.getAllRecipes()) {
            if (!contains(recipe.mInputs, Materials.CastIron)) continue;
            helper.assertTrue(
                !contains(recipe.mOutputs, Materials.IronMagnetic),
                "Cast Iron still becomes Magnetic Iron");
        }
        for (GTRecipe recipe : RecipeMaps.alloySmelterRecipes.getAllRecipes()) {
            if (!contains(recipe.mInputs, Materials.CastIron)) continue;
            for (ItemStack output : recipe.mOutputs) helper
                .assertTrue(!new ItemStack(Blocks.anvil).isItemEqual(output), "Cast Iron still becomes an Iron anvil");
        }
        for (GTRecipe recipe : RecipeMaps.extruderRecipes.getAllRecipes()) {
            if (!contains(recipe.mInputs, Materials.CastIron)) continue;
            for (ItemStack output : recipe.mOutputs) helper.assertTrue(
                !ItemList.IC2_Fuel_Rod_Empty.get(1)
                    .isItemEqual(output),
                "Cast Iron still becomes an Iron fuel rod");
        }
        helper.succeed();
    }

    private static boolean contains(ItemStack[] stacks, Materials material) {
        if (stacks == null) return false;
        for (ItemStack stack : stacks) {
            ItemData data = GTOreDictUnificator.getItemData(stack);
            if (data != null && data.mMaterial != null && data.mMaterial.mMaterial == material) return true;
        }
        return false;
    }
}
