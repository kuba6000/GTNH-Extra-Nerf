package pl.kuba6000.gtnhextranerf.integration;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;
import gtnhlanth.common.register.WerkstoffMaterialPool;

/** Checks the final recipes available to single-block and industrial machines. */
final class IndustrialRecipeIntegrationTests {

    private IndustrialRecipeIntegrationTests() {}

    static void verify() {
        verifyRubyJuiceRemoved();
        verifyNativeGalliumYields();
        verifyIndustrialRecipesMatchTheirNerfedCounterparts();
    }

    private static void verifyNativeGalliumYields() {
        ItemStack gallium = Materials.Gallium.getDust(1);
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.centrifugeRecipes,
            RecipeMaps.centrifugeNonCellRecipes }) {
            GTRecipe recipe = map.findRecipeQuery()
                .items(Materials.IlmeniteSlag.getDust(1))
                .find();
            check(recipe != null, "Ilmenite Slag recipe is missing from " + map.unlocalizedName);
            check(
                Arrays.stream(recipe.mOutputs)
                    .noneMatch(gallium::isItemEqual),
                "Ilmenite Slag still produces added Gallium in " + map.unlocalizedName);
            check(recipe.mDuration == 10 * SECONDS, "Ilmenite Slag processing time changed in " + map.unlocalizedName);
        }
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.electrolyzerRecipes,
            RecipeMaps.electrolyzerNonCellRecipes }) {
            GTRecipe recipe = map.findRecipeQuery()
                .items(Materials.Sphalerite.getDust(2))
                .find();
            check(recipe != null, "Sphalerite recipe is missing from " + map.unlocalizedName);
            int galliumSlot = -1, galliumOutputs = 0;
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                if (gallium.isItemEqual(recipe.mOutputs[i])) {
                    galliumSlot = i;
                    galliumOutputs++;
                }
            }
            check(
                galliumOutputs == 1 && recipe.mOutputs[galliumSlot].stackSize == 1
                    && recipe.getOutputChance(galliumSlot) == 917,
                "Sphalerite does not use the original Gallium chance in " + map.unlocalizedName);
        }
    }

    private static void verifyIndustrialRecipesMatchTheirNerfedCounterparts() {
        equivalent(
            "Ilmenite Slag",
            RecipeMaps.centrifugeRecipes,
            RecipeMaps.centrifugeNonCellRecipes,
            new ItemStack[] { Materials.IlmeniteSlag.getDust(1) });
        equivalent(
            "Endstone",
            RecipeMaps.centrifugeRecipes,
            RecipeMaps.centrifugeNonCellRecipes,
            new ItemStack[] { Materials.Endstone.getDust(36) });
        equivalent(
            "Samarium concentrate",
            RecipeMaps.centrifugeRecipes,
            RecipeMaps.centrifugeNonCellRecipes,
            new ItemStack[] { WerkstoffMaterialPool.DephosphatedSamariumConcentrate.get(OrePrefixes.dust, 6) });
        equivalent(
            "small Samarium blend",
            RecipeMaps.electrolyzerRecipes,
            RecipeMaps.electrolyzerNonCellRecipes,
            new ItemStack[] { WerkstoffMaterialPool.SamariumChlorideSodiumChlorideBlend.get(OrePrefixes.dust, 6),
                GTUtility.getIntegratedCircuit(1) });
        equivalent(
            "large Samarium blend",
            RecipeMaps.electrolyzerRecipes,
            RecipeMaps.electrolyzerNonCellRecipes,
            new ItemStack[] { WerkstoffMaterialPool.SamariumChlorideSodiumChlorideBlend.get(OrePrefixes.dust, 54),
                GTUtility.getIntegratedCircuit(9) });
        equivalent(
            "Sphalerite",
            RecipeMaps.electrolyzerRecipes,
            RecipeMaps.electrolyzerNonCellRecipes,
            new ItemStack[] { Materials.Sphalerite.getDust(2) });
        equivalent(
            "Raw Neutronium",
            RecipeMaps.mixerRecipes,
            RecipeMaps.mixerNonCellRecipes,
            new ItemStack[] { Materials.Neutronium.getDust(1), GTUtility.getIntegratedCircuit(1) },
            Materials.Helium.getPlasma(1_440));
        if (Mods.Forestry.isModLoaded()) {
            equivalent(
                "Indium Comb",
                RecipeMaps.centrifugeRecipes,
                RecipeMaps.centrifugeNonCellRecipes,
                new ItemStack[] { GTBees.combs.getStackForType(CombType.INDIUM, 8), GTUtility.getIntegratedCircuit(2) },
                GGMaterial.thoriumBasedLiquidFuelDepleted.getFluidOrGas(1_000));
        }
    }

    private static void equivalent(String name, RecipeMap<?> singleMap, RecipeMap<?> industrialMap, ItemStack[] items,
        FluidStack... fluids) {
        GTRecipe single = singleMap.findRecipeQuery()
            .items(items)
            .fluids(fluids)
            .find();
        GTRecipe industrial = industrialMap.findRecipeQuery()
            .items(items)
            .fluids(fluids)
            .find();
        check(single != null && industrial != null, name + " must be available in both machine types");
        check(
            single.mDuration == industrial.mDuration && single.mEUt == industrial.mEUt,
            name + " has different base time or voltage in the industrial machine");
        check(single.mOutputs.length == industrial.mOutputs.length, name + " has different item outputs");
        for (int i = 0; i < single.mOutputs.length; i++) {
            check(
                ItemStack.areItemStacksEqual(single.mOutputs[i], industrial.mOutputs[i])
                    && single.getOutputChance(i) == industrial.getOutputChance(i),
                name + " has a different item output or chance in the industrial machine");
        }
        check(sameFluids(single.mFluidInputs, industrial.mFluidInputs), name + " has a different fluid cost");
        check(sameFluids(single.mFluidOutputs, industrial.mFluidOutputs), name + " has different fluid outputs");
    }

    private static boolean sameFluids(FluidStack[] first, FluidStack[] second) {
        if (first.length != second.length) return false;
        for (int i = 0; i < first.length; i++) {
            if (!first[i].isFluidEqual(second[i]) || first[i].amount != second[i].amount) return false;
        }
        return true;
    }

    private static void verifyRubyJuiceRemoved() {
        FluidStack juice = Materials.RubyJuice.getFluid(1);
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.mixerRecipes, RecipeMaps.mixerNonCellRecipes }) {
            check(
                map.getAllRecipes()
                    .stream()
                    .noneMatch(recipe -> contains(recipe.mFluidOutputs, juice)),
                "Ruby Juice can still be made in " + map.unlocalizedName);
        }
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.centrifugeRecipes,
            RecipeMaps.centrifugeNonCellRecipes }) {
            check(
                map.getAllRecipes()
                    .stream()
                    .noneMatch(recipe -> contains(recipe.mFluidInputs, juice)),
                "Ruby Juice can still be processed in " + map.unlocalizedName);
        }
    }

    private static boolean contains(FluidStack[] fluids, FluidStack target) {
        return fluids != null && Arrays.stream(fluids)
            .anyMatch(fluid -> fluid != null && fluid.isFluidEqual(target));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
