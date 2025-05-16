package com.kuba6000.gtnhextranerf.loader.recipes;

import static gregtech.api.recipe.RecipeMaps.chemicalBathRecipes;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public class AluminiumNerf {

    public static void run() {

        /*
         * GTValues.RA.stdBuilder()
         * .itemInputs(GTOreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Ilmenite, 1))
         * .itemOutputs(Materials.Rutile.getDust(2), MaterialsOreAlum.IlmeniteSlag.getDust(1))
         * .outputChances(10000, 3000)
         * .fluidInputs(Materials.SulfuricAcid.getFluid(1000))
         * .fluidOutputs(new FluidStack(ItemList.sGreenVitriol, 2000))
         * .duration(21 * SECONDS)
         * .eut(1000)
         * .addTo(chemicalBathRecipes);
         */

        chemicalBathRecipes.getBackend()
            .removeRecipe(
                chemicalBathRecipes.findRecipeQuery()
                    .items(GTOreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Ilmenite, 1))
                    .fluids(Materials.SulfuricAcid.getFluid(1000))
                    .find());

    }

}
