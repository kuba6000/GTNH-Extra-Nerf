package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

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
        Materials[] metals = { Materials.WroughtIron, Materials.CastIron, Materials.PigIron,
            IronBathMaterials.overcarburizedIron() };
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
            assertFurnaceSmeltsToIngot(helper, dust, ingot);
            assertFluidExtraction(helper, material, ingot);
            assertFluidExtraction(helper, material, dust);
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

    @GameTest(template = "native_fixture")
    public static void wroughtIronFormsRecycleWithoutChangingMaterial(GameTestHelper helper) {
        Materials material = IronBathMaterials.wroughtIron();
        ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dust, material, 1);
        ItemStack ingot = GTOreDictUnificator.get(OrePrefixes.ingot, material, 1);
        ItemStack plate = GTOreDictUnificator.get(OrePrefixes.plate, material, 1);
        assertFurnaceSmeltsToIngot(helper, plate, ingot);
        assertFluidExtraction(helper, material, plate);
        helper.assertTrue(
            RecipeMaps.maceratorRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    RecipeMatch.builder()
                        .itemInputs(plate)
                        .itemOutputs(dust)
                        .build()),
            "Wrought Iron plate does not macerate into one Wrought Iron dust");
        helper.succeed();
    }

    @GameTest(template = "native_fixture")
    public static void bathMetalsHaveStandardPartsAndCastings(GameTestHelper helper) {
        Materials[] metals = { Materials.WroughtIron, IronBathMaterials.overcarburizedIron(), Materials.CastIron,
            Materials.PigIron };
        OrePrefixes[] parts = { OrePrefixes.dustTiny, OrePrefixes.dustSmall, OrePrefixes.nugget, OrePrefixes.block,
            OrePrefixes.plate, OrePrefixes.plateDouble, OrePrefixes.plateTriple, OrePrefixes.plateQuadruple,
            OrePrefixes.plateQuintuple, OrePrefixes.plateDense, OrePrefixes.plateSuperdense, OrePrefixes.itemCasing,
            OrePrefixes.stick, OrePrefixes.stickLong, OrePrefixes.foil, OrePrefixes.bolt, OrePrefixes.screw,
            OrePrefixes.ring, OrePrefixes.round, OrePrefixes.spring, OrePrefixes.springSmall, OrePrefixes.wireFine,
            OrePrefixes.gearGt, OrePrefixes.gearGtSmall, OrePrefixes.rotor, OrePrefixes.toolHeadHammer,
            OrePrefixes.turbineBlade, OrePrefixes.frameGt };
        for (Materials metal : metals) {
            for (OrePrefixes prefix : parts) {
                ItemStack stack = GTOreDictUnificator.get(prefix, metal, 1);
                ItemData data = GTOreDictUnificator.getItemData(stack);
                helper.assertTrue(
                    data != null && data.mMaterial != null && data.mMaterial.mMaterial == metal,
                    metal + " has no independent " + prefix);
            }
            assertCasting(helper, metal, OrePrefixes.nugget, ItemList.Shape_Mold_Nugget, 16);
            assertCasting(helper, metal, OrePrefixes.ingot, ItemList.Shape_Mold_Ingot, 144);
            assertCasting(helper, metal, OrePrefixes.block, ItemList.Shape_Mold_Block, 1296);
            assertCasting(helper, metal, OrePrefixes.plate, ItemList.Shape_Mold_Plate, 144);
            assertCasting(helper, metal, OrePrefixes.itemCasing, ItemList.Shape_Mold_Casing, 72);
            assertCasting(helper, metal, OrePrefixes.gearGt, ItemList.Shape_Mold_Gear, 576);
            assertCasting(helper, metal, OrePrefixes.gearGtSmall, ItemList.Shape_Mold_Gear_Small, 144);
            assertCasting(helper, metal, OrePrefixes.rotor, ItemList.Shape_Mold_Rotor, 612);
            assertCasting(helper, metal, OrePrefixes.ring, ItemList.Shape_Mold_Ring, 36);
            assertCasting(helper, metal, OrePrefixes.screw, ItemList.Shape_Mold_Screw, 18);
            assertCasting(helper, metal, OrePrefixes.stick, ItemList.Shape_Mold_Rod, 72);
            assertCasting(helper, metal, OrePrefixes.bolt, ItemList.Shape_Mold_Bolt, 18);
            assertCasting(helper, metal, OrePrefixes.round, ItemList.Shape_Mold_Round, 16);
            assertCasting(helper, metal, OrePrefixes.stickLong, ItemList.Shape_Mold_Rod_Long, 144);
            assertCasting(helper, metal, OrePrefixes.turbineBlade, ItemList.Shape_Mold_Turbine_Blade, 864);
        }
        helper.succeed();
    }

    private static void assertCasting(GameTestHelper helper, Materials metal, OrePrefixes prefix, ItemList mold,
        int fluidAmount) {
        GTRecipe casting = RecipeMaps.fluidSolidifierRecipes.findRecipeQuery()
            .items(mold.get(1))
            .fluids(metal.getMolten(fluidAmount))
            .voltage(Long.MAX_VALUE)
            .find();
        ItemStack part = GTOreDictUnificator.get(prefix, metal, 1);
        helper.assertTrue(
            casting != null && casting.mOutputs.length == 1
                && casting.mOutputs[0].stackSize == 1
                && casting.mOutputs[0].isItemEqual(part)
                && casting.mFluidInputs.length == 1
                && casting.mFluidInputs[0].amount == fluidAmount,
            metal + " does not cast into one " + prefix + " from " + fluidAmount + " mB");
    }

    private static void assertFurnaceSmeltsToIngot(GameTestHelper helper, ItemStack input, ItemStack ingot) {
        ItemStack smelted = FurnaceRecipes.smelting()
            .getSmeltingResult(input);
        helper.assertTrue(
            smelted != null && smelted.stackSize == 1 && smelted.isItemEqual(ingot),
            input + " does not smelt into one ingot of the same metal");
    }

    private static void assertFluidExtraction(GameTestHelper helper, Materials material, ItemStack input) {
        helper.assertTrue(
            RecipeMaps.fluidExtractionRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    RecipeMatch.builder()
                        .itemInputs(input)
                        .fluidOutputs(material.getMolten(144))
                        .build()),
            input + " does not extract into 144 mB of molten " + material);
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
