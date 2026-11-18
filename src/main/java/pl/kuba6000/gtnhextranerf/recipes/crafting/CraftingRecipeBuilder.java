package pl.kuba6000.gtnhextranerf.recipes.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.item.ItemStack;

/** Describes both the crafting target and the recipe to construct. */
public final class CraftingRecipeBuilder {

    enum Kind {

        SHAPED,
        SHAPELESS,
        VANILLA_SHAPELESS,
        UNIVERSAL,
        UNIVERSAL_SHAPELESS;

        boolean isShapeless() {
            return this == SHAPELESS || this == VANILLA_SHAPELESS || this == UNIVERSAL_SHAPELESS;
        }
    }

    final Kind kind;
    String[] pattern = new String[0];
    final Map<Character, CraftingIngredient> ingredients = new LinkedHashMap<>();
    final List<CraftingIngredient> inputs = new ArrayList<>();
    ItemStack output;
    boolean removable = true;
    boolean mirrored;
    CraftingRecipeType matchType;

    private CraftingRecipeBuilder(Kind kind) {
        this.kind = kind;
    }

    CraftingRecipeBuilder(CraftingDefinition source) {
        kind = source.kind;
        pattern = source.pattern.toArray(new String[0]);
        ingredients.putAll(source.ingredients);
        inputs.addAll(source.inputs);
        output = source.output();
        removable = source.removable;
        mirrored = source.mirrored;
        matchType = source.matchType;
    }

    public static CraftingRecipeBuilder shaped() {
        return new CraftingRecipeBuilder(Kind.SHAPED);
    }

    public static CraftingRecipeBuilder shapeless() {
        return new CraftingRecipeBuilder(Kind.SHAPELESS);
    }

    public static CraftingRecipeBuilder vanillaShapeless() {
        return new CraftingRecipeBuilder(Kind.VANILLA_SHAPELESS);
    }

    public static CraftingRecipeBuilder universalShaped() {
        return new CraftingRecipeBuilder(Kind.UNIVERSAL);
    }

    public static CraftingRecipeBuilder universalShapeless() {
        return new CraftingRecipeBuilder(Kind.UNIVERSAL_SHAPELESS);
    }

    public CraftingRecipeBuilder pattern(String... rows) {
        if (kind.isShapeless()) throw new IllegalArgumentException("Shapeless recipes have no pattern");
        pattern = rows.clone();
        return this;
    }

    public CraftingRecipeBuilder ingredient(char symbol, ItemStack item) {
        return ingredient(symbol, CraftingIngredient.item(item));
    }

    public CraftingRecipeBuilder ingredient(char symbol, String oreName) {
        return ingredient(symbol, CraftingIngredient.ore(oreName));
    }

    public CraftingRecipeBuilder ingredient(char symbol, CraftingIngredient value) {
        if (kind.isShapeless() || symbol == ' ') throw new IllegalArgumentException("Invalid pattern binding");
        ingredients.put(symbol, Objects.requireNonNull(value, "Ingredient cannot be null"));
        return this;
    }

    public CraftingRecipeBuilder inputs(ItemStack... items) {
        if (!kind.isShapeless()) throw new IllegalArgumentException("Use pattern bindings for shaped recipes");
        inputs.clear();
        for (ItemStack item : items) inputs.add(CraftingIngredient.item(item));
        return this;
    }

    public CraftingRecipeBuilder ingredients(CraftingIngredient... values) {
        if (!kind.isShapeless()) throw new IllegalArgumentException("Use pattern bindings for shaped recipes");
        inputs.clear();
        for (CraftingIngredient value : values) inputs.add(Objects.requireNonNull(value, "Ingredient cannot be null"));
        return this;
    }

    public CraftingRecipeBuilder output(ItemStack item) {
        output = checked(item).copy();
        return this;
    }

    public CraftingRecipeBuilder removable(boolean value) {
        if (kind != Kind.SHAPED && kind != Kind.SHAPELESS)
            throw new IllegalArgumentException("Removability is configurable only for GT recipes");
        removable = value;
        return this;
    }

    public CraftingRecipeBuilder mirrored(boolean value) {
        if (kind != Kind.SHAPED)
            throw new IllegalArgumentException("Mirroring is configurable only for GT shaped recipes");
        mirrored = value;
        return this;
    }

    public CraftingRecipeBuilder matchType(CraftingRecipeType type) {
        matchType = Objects.requireNonNull(type);
        return this;
    }

    public CraftingDefinition build() {
        checked(output);
        if (kind.isShapeless()) {
            if (inputs.isEmpty() || inputs.size() > 9) throw new IllegalArgumentException("Expected 1..9 ingredients");
        } else {
            if (pattern.length < 1 || pattern.length > 3)
                throw new IllegalArgumentException("Expected 1..3 pattern rows");
            int width = Objects.requireNonNull(pattern[0])
                .length();
            if (width < 1 || width > 3) throw new IllegalArgumentException("Expected 1..3 pattern columns");
            Set<Character> used = new HashSet<>();
            for (String row : pattern) {
                if (row == null || row.length() != width)
                    throw new IllegalArgumentException("Pattern must be rectangular");
                for (char symbol : row.toCharArray()) if (symbol != ' ') used.add(symbol);
            }
            if (used.isEmpty() || !used.equals(ingredients.keySet()))
                throw new IllegalArgumentException("Pattern bindings must match used symbols exactly");
        }
        return new CraftingDefinition(this);
    }

    private static ItemStack checked(ItemStack item) {
        if (item == null || item.getItem() == null || item.stackSize <= 0)
            throw new IllegalArgumentException("Expected a nonempty item stack");
        return item;
    }

}
