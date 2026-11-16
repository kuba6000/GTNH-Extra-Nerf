package pl.kuba6000.gtnhextranerf.loader.recipes;

import static pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingIngredient.ore;

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.materials.IronBathMaterials;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingDefinition;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingIngredient;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeBuilder;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeType;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

/** Gives the bath's iron products recurring uses in native alloy production. */
public final class IronBathAlloyRecipes implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.MODIFY_IRON_BATH_ALLOY_MIXTURES, IronBathAlloyRecipes::mixers);
        registry.register(AuditScope.MODIFY_IRON_BATH_ALLOY_HAND_CRAFTING, IronBathAlloyRecipes::crafting);
        registry.register(AuditScope.REMOVE_AMBIGUOUS_SHAFT_ALLOY_RECYCLING, IronBathAlloyRecipes::shaftRecycling);
    }

    private static void mixers(RecipeContext recipes) {
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.mixerRecipes, RecipeMaps.mixerNonCellRecipes }) {
            stainless(recipes, map, 6, 2, dust(Materials.Nickel, 1));
            stainless(recipes, map, 4, 1, dust(Materials.Invar, 3));
            addFeed(
                recipes,
                map,
                Materials.ElectricalSteel,
                3,
                100,
                7,
                2,
                Materials.CastIron,
                1,
                dust(Materials.Steel, 1),
                dust(Materials.Coal, 1),
                dust(Materials.Silicon, 1));
            addFeed(
                recipes,
                map,
                Materials.DarkSteel,
                3,
                100,
                7,
                2,
                Materials.PigIron,
                1,
                dust(Materials.ElectricalSteel, 1),
                dust(Materials.Coal, 1),
                dust(Materials.Obsidian, 1));
            addFeed(
                recipes,
                map,
                Materials.DarkSteel,
                9,
                120,
                120,
                14,
                Materials.PigIron,
                3,
                dust(Materials.Steel, 1),
                dust(Materials.Coal, 4),
                dust(Materials.Silicon, 1),
                dust(Materials.Obsidian, 3));
        }
    }

    private static void stainless(RecipeContext recipes, RecipeMap<?> map, int iron, int circuit, ItemStack alloy) {
        ItemStack source = dust(Materials.Iron, iron);
        recipes.machine(map)
            .modifyOne(
                match(
                    Materials.StainlessSteel,
                    9,
                    900,
                    120,
                    source,
                    alloy,
                    dust(Materials.Manganese, 1),
                    dust(Materials.Chrome, 1),
                    GTUtility.getIntegratedCircuit(circuit)),
                RecipeChange.builder()
                    .replaceItemInput(source, dust(IronBathMaterials.wroughtIron(), iron))
                    .build());
    }

    private static void addFeed(RecipeContext recipes, RecipeMap<?> map, Materials output, int amount, int duration,
        int eut, int circuit, Materials feed, int feedAmount, ItemStack... ingredients) {
        ItemStack[] inputs = Arrays.copyOf(ingredients, ingredients.length + 1);
        inputs[ingredients.length] = GTUtility.getIntegratedCircuit(circuit);
        recipes.machine(map)
            .modifyOne(
                match(output, amount, duration, eut, inputs),
                RecipeChange.builder()
                    .addItemInput(dust(feed, feedAmount))
                    .build());
    }

    private static RecipeMatch match(Materials output, int amount, int duration, int eut, ItemStack... inputs) {
        return RecipeMatch.builder()
            .itemInputs(inputs)
            .itemOutputs(dust(output, amount))
            .fluidInputs()
            .fluidOutputs()
            .duration(duration)
            .eut(eut)
            .build();
    }

    private static void crafting(RecipeContext recipes) {
        addHandFeed(
            recipes,
            Materials.ElectricalSteel,
            Materials.CastIron,
            ore("dustSteel"),
            ore("dustCoal"),
            ore("dustSilicon"));
        addHandFeed(
            recipes,
            Materials.DarkSteel,
            Materials.PigIron,
            ore("dustElectricalSteel"),
            ore("dustCoal"),
            ore("dustObsidian"));
    }

    private static void addHandFeed(RecipeContext recipes, Materials output, Materials feed,
        CraftingIngredient... ingredients) {
        CraftingDefinition original = CraftingRecipeBuilder.shapeless()
            .ingredients(ingredients)
            .output(dust(output, 2))
            .matchType(CraftingRecipeType.GT_SHAPELESS)
            .build();
        CraftingIngredient[] replacement = Arrays.copyOf(ingredients, ingredients.length + 1);
        replacement[ingredients.length] = ore("dust" + feed.mName);
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredients(replacement)
                    .build());
    }

    private static ItemStack dust(Materials material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.dust, material, count);
    }

    private static void shaftRecycling(RecipeContext recipes) {
        ItemStack shaft = GregtechItemList.Shape_Extruder_WindmillShaft.get(1);
        recipes.machine(RecipeMaps.arcFurnaceRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(shaft, GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.nugget, Materials.DarkSteel, 37))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(138)
                    .eut(30)
                    .build(),
                1);
        recipes.machine(RecipeMaps.fluidExtractionRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(shaft)
                    .itemOutputs()
                    .fluidInputs()
                    .fluidOutputs(Materials.DarkSteel.getMolten(594))
                    .duration(99)
                    .eut(40)
                    .build(),
                1);
        recipes.machine(RecipeMaps.maceratorRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(shaft)
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dustTiny, Materials.DarkSteel, 37))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(111)
                    .eut(4)
                    .build(),
                1);
    }
}
