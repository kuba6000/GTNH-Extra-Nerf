package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;
import gregtech.loaders.postload.recipes.CuttingRecipes;

/** Test-only registrations made before the cutter's NEI recipes are assembled. */
final class CutterCoolantFixture {

    static final int WATER_ONLY_CIRCUIT = 17;
    static final int SEPARATE_VARIANTS_CIRCUIT = 18;
    static final int AUTOMATIC_VARIANTS_CIRCUIT = 19;
    static final int CLASSIC_VARIANTS_CIRCUIT = 20;

    private CutterCoolantFixture() {}

    static void register() {
        addWater(WATER_ONLY_CIRCUIT, new ItemStack(Items.book));
        addWater(SEPARATE_VARIANTS_CIRCUIT, new ItemStack(Items.arrow));
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.nether_star))
            .circuit(SEPARATE_VARIANTS_CIRCUIT)
            .itemOutputs(new ItemStack(Items.arrow))
            .fluidInputs(Materials.Lubricant.getFluid(10))
            .duration(40)
            .eut(8)
            .ignoreCollision()
            .addTo(RecipeMaps.cutterRecipes);
        new CuttingRecipes().recipeWithClassicFluids(
            new ItemStack[] { new ItemStack(Items.nether_star),
                GTUtility.getIntegratedCircuit(CLASSIC_VARIANTS_CIRCUIT) },
            new ItemStack[] { new ItemStack(Items.egg) },
            40,
            8,
            false);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.nether_star))
            .circuit(AUTOMATIC_VARIANTS_CIRCUIT)
            .itemOutputs(new ItemStack(Items.feather))
            .duration(40)
            .eut(8)
            .ignoreCollision()
            .addTo(RecipeMaps.cutterRecipes);
    }

    static ItemStack circuit(int configuration) {
        return ItemList.Circuit_Integrated.getWithDamage(1, configuration);
    }

    private static void addWater(int configuration, ItemStack output) {
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.nether_star))
            .circuit(configuration)
            .itemOutputs(output)
            .fluidInputs(Materials.Water.getFluid(100))
            .duration(40)
            .eut(8)
            .ignoreCollision()
            .addTo(RecipeMaps.cutterRecipes);
    }
}
