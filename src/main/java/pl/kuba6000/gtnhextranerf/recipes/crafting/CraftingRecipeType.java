package pl.kuba6000.gtnhextranerf.recipes.crafting;

import net.minecraft.item.crafting.ShapelessRecipes;

import com.dreammaster.recipes.ShapedUniversalRecipe;
import com.dreammaster.recipes.ShapelessUniversalRecipe;

import gregtech.api.util.GTShapedRecipe;
import gregtech.api.util.GTShapelessRecipe;

public enum CraftingRecipeType {

    VANILLA_SHAPELESS(ShapelessRecipes.class),
    GT_SHAPED(GTShapedRecipe.class),
    GT_SHAPELESS(GTShapelessRecipe.class),
    UNIVERSAL_SHAPED(ShapedUniversalRecipe.class),
    UNIVERSAL_SHAPELESS(ShapelessUniversalRecipe.class);

    private final Class<?> type;

    CraftingRecipeType(Class<?> type) {
        this.type = type;
    }

    boolean matches(CraftingRecipe recipe) {
        return recipe.handle()
            .getClass() == type;
    }
}
