package pl.kuba6000.gtnhextranerf.loader.recipes;

import static gregtech.api.recipe.RecipeMaps.centrifugeNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalBathRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeBuilder.TICKS;

import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

public final class AluminiumNerf implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.REMOVE_ILMENITE_CHEMICAL_BATH_RECIPES, AluminiumNerf::removeIlmeniteChemicalBath);
        registry.register(AuditScope.REMOVE_RUBY_JUICE_RECIPES, AluminiumNerf::removeRubyJuiceRecipes);
        registry
            .register(AuditScope.MODIFY_ILMENITE_SLAG_CENTRIFUGE_RECIPE, AluminiumNerf::modifyIlmeniteSlagCentrifuge);
    }

    private static void removeIlmeniteChemicalBath(RecipeContext recipes) {
        removeIlmeniteChemicalBath(recipes, OrePrefixes.crushedPurified, 3000);
        removeIlmeniteChemicalBath(recipes, OrePrefixes.crushed, 6000);
    }

    private static void removeIlmeniteChemicalBath(RecipeContext recipes, OrePrefixes prefix, int slagChance) {
        recipes.machine(chemicalBathRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(GTOreDictUnificator.get(prefix, Materials.Ilmenite, 1))
                    .itemOutputs(Materials.Rutile.getDust(2), Materials.IlmeniteSlag.getDust(1))
                    .fluidInputs(Materials.SulfuricAcid.getFluid(1_000))
                    .fluidOutputs(new FluidStack(ItemList.sGreenVitriol, 2_000))
                    .outputChances(10000, slagChance)
                    .duration(21 * SECONDS)
                    .eut(1000)
                    .build(),
                1);
    }

    private static void removeRubyJuiceRecipes(RecipeContext recipes) {
        OrePrefixes[] washable = { OrePrefixes.crushed, OrePrefixes.crushedPurified, OrePrefixes.dustImpure,
            OrePrefixes.dustPure };

        for (OrePrefixes prefix : washable) {
            removeRubyMixerRecipe(recipes, prefix, 1, OrePrefixes.dustTiny, 1, 1_000, 1, 2 * SECONDS, TierEU.RECIPE_MV);
            removeRubyMixerRecipe(recipes, prefix, 9, OrePrefixes.dust, 1, 9_000, 9, 3 * SECONDS, TierEU.RECIPE_HV);
        }

        RecipeMatch centrifuge = RecipeMatch.builder()
            .itemInputs(GTUtility.getIntegratedCircuit(1))
            .itemOutputs(
                Materials.Aluminiumhydroxide.getDust(2),
                Materials.Chrome.getDust(1),
                Materials.Iron.getDust(1),
                Materials.Vanadium.getDust(1),
                Materials.Magnesium.getDust(1))
            .fluidInputs(Materials.RubyJuice.getFluid(1_000))
            .fluidOutputs(Materials.HydrochloricAcid.getFluid(1_000))
            .outputChances(10000, 5000, 300, 200, 200)
            .duration(2 * SECONDS + 5 * TICKS)
            .eut(100)
            .build();
        for (RecipeMap<?> map : new RecipeMap<?>[] { centrifugeRecipes, centrifugeNonCellRecipes }) {
            recipes.machine(map)
                .removeExactly(centrifuge, 1);
        }
    }

    private static void removeRubyMixerRecipe(RecipeContext recipes, OrePrefixes rubyPrefix, int rubyAmount,
        OrePrefixes sodiumHydroxidePrefix, int sodiumHydroxideAmount, int fluidAmount, int circuit, int duration,
        long eut) {
        RecipeMatch mixer = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(rubyPrefix, Materials.Ruby, rubyAmount),
                GTOreDictUnificator.get(sodiumHydroxidePrefix, Materials.SodiumHydroxide, sodiumHydroxideAmount),
                GTUtility.getIntegratedCircuit(circuit))
            .itemOutputs()
            .fluidInputs(Materials.HydrochloricAcid.getFluid(fluidAmount))
            .fluidOutputs(Materials.RubyJuice.getFluid(fluidAmount))
            .duration(duration)
            .eut(eut)
            .build();
        for (RecipeMap<?> map : new RecipeMap<?>[] { mixerRecipes, mixerNonCellRecipes }) {
            recipes.machine(map)
                .removeExactly(mixer, 1);
        }
    }

    private static void modifyIlmeniteSlagCentrifuge(RecipeContext recipes) {
        for (RecipeMap<?> map : new RecipeMap<?>[] { centrifugeRecipes, centrifugeNonCellRecipes }) {
            recipes.machine(map)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(Materials.IlmeniteSlag.getDust(1))
                        .itemOutputs(
                            Materials.Iron.getDust(1),
                            Materials.Niobium.getDust(1),
                            Materials.Tantalum.getDust(1),
                            Materials.Manganese.getDust(1),
                            Materials.Magnesium.getDust(1))
                        .fluidInputs()
                        .fluidOutputs()
                        .outputChances(8000, 500, 2000, 5000, 6000)
                        .duration(2 * SECONDS)
                        .eut(TierEU.RECIPE_MV)
                        .build(),
                    RecipeChange.builder()
                        .appendItemOutput(Materials.Gallium.getDust(1), 1000)
                        .duration(10 * SECONDS)
                        .build());
        }
    }
}
