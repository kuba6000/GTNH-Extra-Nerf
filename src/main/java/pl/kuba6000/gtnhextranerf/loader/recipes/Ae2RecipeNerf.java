package pl.kuba6000.gtnhextranerf.loader.recipes;

import static pl.kuba6000.gtnhextranerf.recipes.RecipeItems.requireModItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.mixins.support.Ae2RecipeIngredients;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingDefinition;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeBuilder;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeType;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

public final class Ae2RecipeNerf implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.AE2_REMOVE_CONTROLLER, Ae2RecipeNerf::removeController);
        registry.register(AuditScope.AE2_TPV_MACHINES, Ae2RecipeNerf::machineMaterials);
        registry.register(AuditScope.AE2_SUNNARIUM_PATTERN, Ae2RecipeNerf::blankPattern);
        registry.register(AuditScope.AE2_REQUIRE_PROCESSING_PATTERN_TERMINAL, Ae2RecipeNerf::universalWirelessTerminal);
    }

    private static void removeController(RecipeContext recipes) {
        ItemStack controller = requireModItem(Mods.AppliedEnergistics2, "tile.BlockController", 1, 0);
        recipes.crafting()
            .removeOne(
                CraftingRecipeBuilder.universalShaped()
                    .pattern("PCP", "EFE", "PCP")
                    .ingredient(
                        'P',
                        OrePrefixes.plate.get(Materials.TPV)
                            .toString())
                    .ingredient('C', "circuitAdvanced")
                    .ingredient('E', requireModItem(Mods.AppliedEnergistics2, "item.ItemMultiMaterial", 1, 24))
                    .ingredient('F', requireModItem(Mods.AppliedEnergistics2, "tile.BlockFluix", 1, 0))
                    .output(controller)
                    .matchType(CraftingRecipeType.UNIVERSAL_SHAPED)
                    .build());
        recipes.machine(RecipeMaps.assemblerRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 4),
                        GTOreDictUnificator.get(OrePrefixes.circuit, Materials.HV, 2),
                        requireModItem(Mods.AppliedEnergistics2, "item.ItemMultiMaterial", 2, 24),
                        requireModItem(Mods.AppliedEnergistics2, "tile.BlockFluix", 1, 0),
                        GTUtility.getIntegratedCircuit(2))
                    .itemOutputs(controller)
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(100)
                    .eut(TierEU.RECIPE_HV)
                    .build(),
                1);
    }

    private static void machineMaterials(RecipeContext recipes) {
        List<RecipeMap<?>> maps = new ArrayList<>(RecipeMap.ALL_RECIPE_MAPS.values());
        maps.sort(Comparator.comparing(map -> map.unlocalizedName));
        for (RecipeMap<?> map : maps) {
            // Edits are staged; the registry is only updated after this modifier finishes.
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (recipe.mOutputs == null || !Arrays.stream(recipe.mOutputs)
                    .anyMatch(Ae2RecipeIngredients::isAe2)) continue;
                RecipeChange.Builder changes = RecipeChange.builder();
                boolean matched = false;
                for (ItemStack input : recipe.mInputs) {
                    if (Ae2RecipeIngredients.isTitanium(input)) {
                        changes.replaceItemInput(input, Ae2RecipeIngredients.tpvItem(input));
                        matched = true;
                    }
                }
                for (FluidStack input : recipe.mFluidInputs) {
                    FluidStack replacement = Ae2RecipeIngredients.tpvFluid(input);
                    if (replacement != null) {
                        changes.replaceFluidInput(input, replacement);
                        matched = true;
                    }
                }
                if (matched) recipes.machine(map)
                    .modifyKnownOne(recipe, changes.build());
            }
        }
    }

    private static void universalWirelessTerminal(RecipeContext recipes) {
        List<ItemStack> inputs = new ArrayList<>(
            Arrays.asList(
                requireModItem(Mods.AppliedEnergistics2, "item.ToolWirelessTerminal", 1, 0),
                requireModItem(Mods.AE2FluidCraft, "wireless_fluid_pattern_terminal", 1, 0),
                requireModItem(Mods.AE2FluidCraft, "wireless_interface_terminal", 1, 0),
                requireModItem(Mods.AE2FluidCraft, "wireless_level_terminal", 1, 0)));
        if (Mods.AE2WCT.isModLoaded()) {
            inputs.add(requireModItem(Mods.AE2WCT, "wirelessCraftingTerminal", 1, 0));
        }
        CraftingDefinition original = CraftingRecipeBuilder.vanillaShapeless()
            .inputs(inputs.toArray(new ItemStack[0]))
            .output(requireModItem(Mods.AE2FluidCraft, "wireless_ultra_terminal", 1, 0))
            .matchType(CraftingRecipeType.VANILLA_SHAPELESS)
            .build();
        inputs.add(requireModItem(Mods.AppliedEnergistics2, "item.ItemMultiPart", 1, 500));
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .inputs(inputs.toArray(new ItemStack[0]))
                    .build());
    }

    private static void blankPattern(RecipeContext recipes) {
        CraftingDefinition original = CraftingRecipeBuilder.universalShaped()
            .pattern("GSG", "SQS", "AAA")
            .ingredient('G', requireModItem(Mods.AppliedEnergistics2, "tile.BlockQuartzGlass", 1, 0))
            .ingredient('S', "plateGlowstone")
            .ingredient('Q', "itemCertusQuartz")
            .ingredient('A', "plateAluminium")
            .output(requireModItem(Mods.AppliedEnergistics2, "item.ItemMultiMaterial", 1, 52))
            .matchType(CraftingRecipeType.UNIVERSAL_SHAPED)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient(
                        'S',
                        OrePrefixes.plate.get(Materials.Sunnarium)
                            .toString())
                    .build());
    }

}
