package pl.kuba6000.gtnhextranerf.recipes.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;

import com.dreammaster.recipes.ShapedUniversalRecipe;
import com.dreammaster.recipes.ShapelessUniversalRecipe;

import gregtech.api.util.GTShapedRecipe;
import gregtech.api.util.GTShapelessRecipe;
import pl.kuba6000.gtnhextranerf.mixins.early.MixinShapedOreRecipeAccessor;

/** Direct constructor calls; no reflection, class names or constructor-signature arrays. */
final class CraftingRecipeFactory {

    private static final CraftingIngredient.Visitor<Object> NATIVE_INGREDIENT = new CraftingIngredient.Visitor<Object>() {

        @Override
        public Object item(ItemStack stack) {
            return stack;
        }

        @Override
        public Object ore(String name) {
            return name;
        }
    };

    private static final CraftingIngredient.Visitor<ItemStack> VANILLA_INGREDIENT = new CraftingIngredient.Visitor<ItemStack>() {

        @Override
        public ItemStack item(ItemStack stack) {
            return stack;
        }

        @Override
        public ItemStack ore(String name) {
            throw new IllegalArgumentException("Vanilla shapeless recipes require item stacks: " + name);
        }
    };

    private CraftingRecipeFactory() {}

    static CraftingRecipe create(CraftingDefinition definition) {
        switch (definition.kind) {
            case UNIVERSAL:
                CraftingIngredient[] ingredients = definition.grid();
                Object[] grid = new Object[ingredients.length];
                for (int i = 0; i < ingredients.length; i++) {
                    if (ingredients[i] != null) grid[i] = ingredients[i].accept(NATIVE_INGREDIENT);
                }
                return new CraftingRecipe(new ShapedUniversalRecipe(definition.output(), grid));
            case VANILLA_SHAPELESS:
                return new CraftingRecipe(
                    new ShapelessRecipes(
                        definition.output(),
                        definition.inputs.stream()
                            .map(ingredient -> ingredient.accept(VANILLA_INGREDIENT))
                            .collect(Collectors.toList())));
            case UNIVERSAL_SHAPELESS:
            case SHAPELESS:
                Object[] inputs = definition.inputs.stream()
                    .map(ingredient -> ingredient.accept(NATIVE_INGREDIENT))
                    .toArray();
                if (definition.kind == CraftingRecipeBuilder.Kind.UNIVERSAL_SHAPELESS) {
                    return new CraftingRecipe(new ShapelessUniversalRecipe(definition.output(), inputs));
                }
                return new CraftingRecipe(
                    new GTShapelessRecipe(definition.output(), definition.removable, false, false, null, null, inputs));
            case SHAPED:
                List<Object> args = new ArrayList<>(definition.pattern);
                definition.ingredients.forEach((symbol, ingredient) -> {
                    args.add(symbol);
                    args.add(ingredient.accept(NATIVE_INGREDIENT));
                });
                Object recipe = new GTShapedRecipe(
                    definition.output(),
                    definition.removable,
                    false,
                    null,
                    null,
                    args.toArray());
                ((MixinShapedOreRecipeAccessor) recipe).gtnhextranerf$setMirrored(definition.mirrored);
                return new CraftingRecipe(recipe);
            default:
                throw new IllegalStateException("Unsupported crafting kind: " + definition.kind);
        }
    }
}
