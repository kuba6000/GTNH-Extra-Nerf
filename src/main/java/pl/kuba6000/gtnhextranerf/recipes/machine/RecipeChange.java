package pl.kuba6000.gtnhextranerf.recipes.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.extensions.ArrayExt;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;

/** Ordered, reusable changes applied by MachineRecipes to a detached recipe. */
public final class RecipeChange implements Consumer<GTRecipe> {

    private final List<Consumer<GTRecipe>> changes;

    private RecipeChange(Builder builder) {
        changes = new ArrayList<>(builder.changes);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void accept(GTRecipe recipe) {
        for (Consumer<GTRecipe> change : changes) change.accept(recipe);
    }

    private static int uniqueItem(ItemStack[] stacks, ItemStack target) {
        int found = -1;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null || stack.getItem() != target.getItem()
                || stack.getItemDamage() != target.getItemDamage()
                || !Objects.equals(stack.getTagCompound(), target.getTagCompound())) continue;
            if (found >= 0) throw new RecipeValidationException("Ambiguous recipe ingredient or output");
            found = i;
        }
        if (found < 0) throw new RecipeValidationException("Missing recipe ingredient or output");
        return found;
    }

    private static int chanceAt(int[] chances, int index) {
        return chances != null && index < chances.length ? chances[index] : 10000;
    }

    private static ItemStack[] copyItems(ItemStack[] items) {
        return Arrays.stream(items)
            .map(
                item -> Objects.requireNonNull(item)
                    .copy())
            .toArray(ItemStack[]::new);
    }

    private static FluidStack[] copyFluids(FluidStack[] fluids) {
        return Arrays.stream(fluids)
            .map(
                fluid -> Objects.requireNonNull(fluid)
                    .copy())
            .toArray(FluidStack[]::new);
    }

    private static int[] remapChances(ItemStack[] old, int[] chances, ItemStack[] replacement) {
        int[] result = new int[replacement.length];
        Arrays.fill(result, 10000);
        for (int i = 0; i < replacement.length; i++) {
            for (int j = 0; j < old.length; j++) {
                ItemStack copy = old[j].copy();
                copy.stackSize = replacement[i].stackSize;
                if (ItemStack.areItemStacksEqual(copy, replacement[i])) {
                    result[i] = chanceAt(chances, uniqueItem(old, replacement[i]));
                    break;
                }
            }
        }
        return result;
    }

    private static int uniqueFluid(FluidStack[] fluids, FluidStack target) {
        int found = -1;
        for (int i = 0; i < fluids.length; i++) {
            if (fluids[i] == null || !fluids[i].isFluidEqual(target)) continue;
            if (found >= 0) throw new RecipeValidationException("Ambiguous recipe fluid");
            found = i;
        }
        if (found < 0) throw new RecipeValidationException("Missing recipe fluid");
        return found;
    }

    private static int[] remapFluidChances(FluidStack[] old, int[] chances, FluidStack[] replacement) {
        int[] result = new int[replacement.length];
        Arrays.fill(result, 10000);
        for (int i = 0; i < replacement.length; i++) {
            for (FluidStack fluid : old) {
                if (fluid.isFluidEqual(replacement[i])) {
                    result[i] = chanceAt(chances, uniqueFluid(old, replacement[i]));
                    break;
                }
            }
        }
        return result;
    }

    private static int scaled(int value, int oldAmount, int amount) {
        if (oldAmount == 0) {
            if (value == 0) return amount;
            throw new RecipeValidationException("Cannot scale a nonconsumable alternative");
        }
        long result = (long) value * amount;
        if (result % oldAmount != 0 || result / oldAmount > Integer.MAX_VALUE) {
            throw new RecipeValidationException("Alternative amount cannot be represented exactly");
        }
        return (int) (result / oldAmount);
    }

    public static final class Builder {

        private final List<Consumer<GTRecipe>> changes = new ArrayList<>();

        private Builder() {}

        public Builder itemInputAmount(ItemStack target, int amount) {
            if (amount < 0) throw new IllegalArgumentException("Input amount must not be negative");
            ItemStack identity = Objects.requireNonNull(target)
                .copy();
            changes.add(recipe -> {
                int slot = uniqueItem(recipe.mInputs, identity);
                int oldAmount = recipe.mInputs[slot].stackSize;
                if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
                    GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
                    if (alt.mOreDictAlt != null && slot < alt.mOreDictAlt.length && alt.mOreDictAlt[slot] != null) {
                        for (ItemStack item : alt.mOreDictAlt[slot])
                            item.stackSize = scaled(item.stackSize, oldAmount, amount);
                    }
                }
                recipe.mInputs[slot].stackSize = amount;
            });
            return this;
        }

        public Builder itemOutputAmount(ItemStack target, int amount) {
            if (amount <= 0) throw new IllegalArgumentException("Output amount must be positive");
            ItemStack identity = Objects.requireNonNull(target)
                .copy();
            changes.add(recipe -> recipe.mOutputs[uniqueItem(recipe.mOutputs, identity)].stackSize = amount);
            return this;
        }

        /** For recipes deliberately splitting the same output across several stacks. */
        public Builder itemOutputAmounts(ItemStack target, int expectedCount, int amount) {
            if (amount <= 0 || expectedCount <= 0)
                throw new IllegalArgumentException("Expected positive amount and count");
            ItemStack identity = Objects.requireNonNull(target)
                .copy();
            changes.add(recipe -> {
                List<ItemStack> matched = new ArrayList<>();
                for (ItemStack output : recipe.mOutputs) {
                    if (output != null && output.getItem() == identity.getItem()
                        && output.getItemDamage() == identity.getItemDamage()
                        && Objects.equals(output.getTagCompound(), identity.getTagCompound())) matched.add(output);
                }
                if (matched.size() != expectedCount)
                    throw new RecipeValidationException("Unexpected number of repeated outputs: " + matched.size());
                for (ItemStack output : matched) output.stackSize = amount;
            });
            return this;
        }

        public Builder outputChance(ItemStack target, int chance) {
            if (chance < 0 || chance > 10000) throw new IllegalArgumentException("Chance must be in 0..10000");
            ItemStack identity = Objects.requireNonNull(target)
                .copy();
            changes.add(recipe -> {
                int slot = uniqueItem(recipe.mOutputs, identity);
                int[] chances = new int[recipe.mOutputs.length];
                for (int i = 0; i < chances.length; i++) {
                    chances[i] = recipe.mOutputChances != null && i < recipe.mOutputChances.length
                        ? recipe.mOutputChances[i]
                        : 10000;
                }
                chances[slot] = chance;
                recipe.mOutputChances = chances;
            });
            return this;
        }

        public Builder duration(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException("Duration must be positive");
            changes.add(recipe -> recipe.mDuration = ticks);
            return this;
        }

        public Builder eut(long value) {
            if (value <= 0 || value > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Recipe EU/t must be positive and fit in an integer");
            changes.add(recipe -> recipe.mEUt = (int) value);
            return this;
        }

        public Builder specialValue(int value) {
            changes.add(recipe -> recipe.mSpecialValue = value);
            return this;
        }

        /** Sets typed metadata on the detached recipe. The supplied value must be immutable. */
        public <T> Builder metadata(RecipeMetadataKey<T> key, T value) {
            Objects.requireNonNull(key, "Metadata key");
            Objects.requireNonNull(value, "Metadata value");
            changes.add(
                recipe -> recipe.getMetadataStorage()
                    .store(key, value));
            return this;
        }

        public Builder replaceItemInput(ItemStack target, ItemStack replacement) {
            ItemStack identity = Objects.requireNonNull(target)
                .copy();
            ItemStack value = Objects.requireNonNull(replacement)
                .copy();
            if (value.stackSize < 0) throw new IllegalArgumentException("Input amount must not be negative");
            changes.add(recipe -> {
                int slot = uniqueItem(recipe.mInputs, identity);
                recipe.mInputs[slot] = value.copy();
                if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
                    GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
                    if (alt.mOreDictAlt != null && slot < alt.mOreDictAlt.length) alt.mOreDictAlt[slot] = null;
                    if (alt.mOreDictIds != null && slot < alt.mOreDictIds.length) alt.mOreDictIds[slot] = -1;
                }
            });
            return this;
        }

        /**
         * Adds a consumed ingredient without replacing existing OreDict alternatives or input chances.
         *
         * @throws IllegalArgumentException if the added count is not positive
         */
        public Builder addItemInput(ItemStack input) {
            ItemStack value = Objects.requireNonNull(input)
                .copy();
            if (value.stackSize <= 0) throw new IllegalArgumentException("Added input must be consumed");
            changes.add(recipe -> {
                int slot = recipe.mInputs.length;
                ItemStack[] inputs = Arrays.copyOf(recipe.mInputs, slot + 1);
                inputs[slot] = value.copy();
                recipe.mInputChances = remapChances(recipe.mInputs, recipe.mInputChances, inputs);
                recipe.mInputChances[slot] = 10000;
                recipe.mInputChances = ArrayExt.fixChancesArray(recipe.mInputChances, -1);
                recipe.mInputs = inputs;
                if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
                    GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
                    if (alt.mOreDictAlt != null) {
                        alt.mOreDictAlt = Arrays.copyOf(alt.mOreDictAlt, slot + 1);
                        alt.mOreDictAlt[slot] = null;
                    }
                    if (alt.mOreDictIds != null) {
                        int previousLength = alt.mOreDictIds.length;
                        alt.mOreDictIds = Arrays.copyOf(alt.mOreDictIds, slot + 1);
                        Arrays.fill(alt.mOreDictIds, Math.min(previousLength, slot), slot + 1, -1);
                    }
                }
            });
            return this;
        }

        /** Replaces the complete input specification with exact stacks. */
        public Builder itemInputs(ItemStack... inputs) {
            ItemStack[] values = copyItems(inputs);
            for (ItemStack value : values)
                if (value.stackSize < 0) throw new IllegalArgumentException("Input amount must not be negative");
            changes.add(recipe -> {
                recipe.mInputChances = remapChances(recipe.mInputs, recipe.mInputChances, values);
                recipe.mInputs = copyItems(values);
                if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
                    GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
                    alt.mOreDictAlt = null;
                    alt.mOreDictIds = null;
                }
            });
            return this;
        }

        public Builder itemOutputs(ItemStack... outputs) {
            ItemStack[] values = copyItems(outputs);
            for (ItemStack value : values)
                if (value.stackSize <= 0) throw new IllegalArgumentException("Output amount must be positive");
            changes.add(recipe -> {
                recipe.mOutputChances = remapChances(recipe.mOutputs, recipe.mOutputChances, values);
                recipe.mOutputs = copyItems(values);
            });
            return this;
        }

        /** Replaces one product while retaining its output chance and every other product. */
        public Builder replaceItemOutput(ItemStack target, ItemStack replacement) {
            ItemStack identity = Objects.requireNonNull(target)
                .copy();
            ItemStack value = Objects.requireNonNull(replacement)
                .copy();
            if (value.stackSize <= 0) throw new IllegalArgumentException("Output amount must be positive");
            changes.add(recipe -> recipe.mOutputs[uniqueItem(recipe.mOutputs, identity)] = value.copy());
            return this;
        }

        public Builder appendItemOutput(ItemStack output, int chance) {
            if (chance < 0 || chance > 10000) throw new IllegalArgumentException("Chance must be in 0..10000");
            ItemStack value = Objects.requireNonNull(output)
                .copy();
            if (value.stackSize <= 0) throw new IllegalArgumentException("Output amount must be positive");
            changes.add(recipe -> {
                int size = recipe.mOutputs.length;
                int[] chances = new int[size + 1];
                for (int i = 0; i < size; i++) chances[i] = chanceAt(recipe.mOutputChances, i);
                chances[size] = chance;
                recipe.mOutputs = Arrays.copyOf(recipe.mOutputs, size + 1);
                recipe.mOutputs[size] = value.copy();
                recipe.mOutputChances = chances;
            });
            return this;
        }

        public Builder fluidInputAmount(FluidStack target, int amount) {
            if (amount <= 0) throw new IllegalArgumentException("Fluid amount must be positive");
            FluidStack identity = Objects.requireNonNull(target)
                .copy();
            changes.add(recipe -> {
                int slot = uniqueFluid(recipe.mFluidInputs, identity);
                int old = recipe.mFluidInputs[slot].amount;
                if (recipe.mAltFluidInputs != null && slot < recipe.mAltFluidInputs.length
                    && recipe.mAltFluidInputs[slot] != null) {
                    for (FluidStack alternative : recipe.mAltFluidInputs[slot])
                        alternative.amount = scaled(alternative.amount, old, amount);
                }
                recipe.mFluidInputs[slot].amount = amount;
            });
            return this;
        }

        public Builder fluidOutputAmount(FluidStack target, int amount) {
            if (amount <= 0) throw new IllegalArgumentException("Fluid amount must be positive");
            FluidStack identity = Objects.requireNonNull(target)
                .copy();
            changes.add(recipe -> recipe.mFluidOutputs[uniqueFluid(recipe.mFluidOutputs, identity)].amount = amount);
            return this;
        }

        /** Replaces one fluid slot without resetting unrelated alternatives or input chances. */
        public Builder replaceFluidInput(FluidStack target, FluidStack replacement) {
            FluidStack identity = Objects.requireNonNull(target)
                .copy();
            FluidStack value = Objects.requireNonNull(replacement)
                .copy();
            if (value.amount <= 0) throw new IllegalArgumentException("Fluid amount must be positive");
            changes.add(recipe -> {
                int slot = uniqueFluid(recipe.mFluidInputs, identity);
                recipe.mFluidInputs[slot] = value.copy();
                if (recipe.mAltFluidInputs != null && slot < recipe.mAltFluidInputs.length)
                    recipe.mAltFluidInputs[slot] = null;
            });
            return this;
        }

        public Builder fluidInputs(FluidStack... inputs) {
            FluidStack[] values = copyFluids(inputs);
            for (FluidStack value : values)
                if (value.amount <= 0) throw new IllegalArgumentException("Fluid amount must be positive");
            changes.add(recipe -> {
                recipe.mFluidInputChances = remapFluidChances(recipe.mFluidInputs, recipe.mFluidInputChances, values);
                recipe.mFluidInputs = copyFluids(values);
                recipe.mAltFluidInputs = null;
            });
            return this;
        }

        public Builder fluidOutputs(FluidStack... outputs) {
            FluidStack[] values = copyFluids(outputs);
            for (FluidStack value : values)
                if (value.amount <= 0) throw new IllegalArgumentException("Fluid amount must be positive");
            changes.add(recipe -> {
                recipe.mFluidOutputChances = remapFluidChances(
                    recipe.mFluidOutputs,
                    recipe.mFluidOutputChances,
                    values);
                recipe.mFluidOutputs = copyFluids(values);
            });
            return this;
        }

        public RecipeChange build() {
            if (changes.isEmpty()) throw new IllegalArgumentException("Declare at least one recipe change");
            return new RecipeChange(this);
        }
    }
}
