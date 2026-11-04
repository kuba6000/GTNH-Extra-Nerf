package pl.kuba6000.gtnhextranerf.integration;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cricketcraft.chisel.api.carving.CarvingUtils;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Tests the final hand and Assembler recipes through native recipe lookup. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class HighTierCasingScenarios {

    private HighTierCasingScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void uevUsesDoublePlates(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("check UEV casing recipes", () -> verifyTier(ItemList.Casing_UEV, Materials.Infinity))
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void uivUsesDoublePlates(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("check UIV casing recipes", () -> verifyTier(ItemList.Casing_UIV, Materials.TranscendentMetal))
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void umvUsesDoublePlates(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("check UMV casing recipes", () -> verifyTier(ItemList.Casing_UMV, Materials.SpaceTime))
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void uxvUsesDenseMhdcsmAndDoubleMagmatter(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("check UXV casing recipes", HighTierCasingScenarios::verifyUxv)
            .thenSucceed();
    }

    private static void verifyTier(ItemList casing, Materials material) {
        ItemStack raw = rawCasing(casing);
        ItemStack wrench = GTOreDictUnificator.getFirstOre("craftingToolWrench", 1);
        check(wrench != null, "A crafting wrench is unavailable");
        ItemStack doublePlate = item(OrePrefixes.plateDouble, material, 1);
        ItemStack oldPlate = item(OrePrefixes.plate, material, 1);
        ItemStack crafted = ring(doublePlate, wrench);
        check(isRaw(crafted, raw), casing + " cannot be crafted with eight double plates");
        check(!isCasing(ring(oldPlate, wrench), raw, casing), casing + " still crafts from ordinary plates");

        ItemStack paid = item(OrePrefixes.plateDouble, material, 8);
        GTRecipe recipe = find(paid, GTUtility.getIntegratedCircuit(8));
        check(produces(recipe, raw), casing + " is missing the double-plate Assembler recipe");
        check(
            recipe.isRecipeInputEqual(true, null, paid, GTUtility.getIntegratedCircuit(8)),
            "Assembler rejects double plates");
        check(paid.stackSize == 0, "Assembler did not consume all eight double plates");
        check(
            !producesCasing(find(item(OrePrefixes.plate, material, 8), GTUtility.getIntegratedCircuit(8)), raw, casing),
            casing + " still assembles from ordinary plates");
        check(
            !produces(find(item(OrePrefixes.plateDouble, material, 7), GTUtility.getIntegratedCircuit(8)), raw),
            casing + " assembles with too few double plates");
    }

    private static void verifyUxv() {
        ItemStack raw = rawCasing(ItemList.Casing_UXV);
        ItemStack wrench = GTOreDictUnificator.getFirstOre("craftingToolWrench", 1);
        check(wrench != null, "A crafting wrench is unavailable");
        ItemStack dense = item(OrePrefixes.plateDense, Materials.MHDCSM, 1);
        ItemStack doubleMagmatter = item(OrePrefixes.plateDouble, Materials.MagMatter, 1);
        ItemStack crafted = LvRecipeIntegrationTests
            .craft(doubleMagmatter, dense, doubleMagmatter, null, wrench, null, doubleMagmatter, null, doubleMagmatter);
        check(isRaw(crafted, raw), "UXV cannot be crafted with dense MHDCSM and double Magmatter");
        ItemStack mhd = item(OrePrefixes.plate, Materials.MHDCSM, 1);
        ItemStack magmatter = item(OrePrefixes.plate, Materials.MagMatter, 1);
        check(
            !isCasing(
                LvRecipeIntegrationTests.craft(mhd, magmatter, mhd, magmatter, wrench, magmatter, mhd, magmatter, mhd),
                raw,
                ItemList.Casing_UXV),
            "UXV still crafts from the old plates");

        ItemStack paidDense = dense.copy();
        ItemStack paidMag = item(OrePrefixes.plateDouble, Materials.MagMatter, 4);
        GTRecipe recipe = find(paidDense, paidMag, GTUtility.getIntegratedCircuit(8));
        check(produces(recipe, raw), "UXV is missing the updated Assembler recipe");
        check(
            recipe.isRecipeInputEqual(true, null, paidDense, paidMag, GTUtility.getIntegratedCircuit(8)),
            "Assembler rejects the new UXV inputs");
        check(paidDense.stackSize == 0 && paidMag.stackSize == 0, "Assembler did not consume the UXV plates");
        check(
            !producesCasing(
                find(
                    item(OrePrefixes.plate, Materials.MHDCSM, 4),
                    item(OrePrefixes.plate, Materials.MagMatter, 4),
                    GTUtility.getIntegratedCircuit(8)),
                raw,
                ItemList.Casing_UXV),
            "UXV still assembles from the old plates");
        check(
            !produces(
                find(dense, item(OrePrefixes.plateDouble, Materials.MagMatter, 3), GTUtility.getIntegratedCircuit(8)),
                raw),
            "UXV assembles with too little Magmatter");
    }

    private static ItemStack ring(ItemStack plate, ItemStack wrench) {
        return LvRecipeIntegrationTests.craft(plate, plate, plate, plate, wrench, plate, plate, plate, plate);
    }

    private static ItemStack rawCasing(ItemList finished) {
        Block rawBlock = GameRegistry.findBlock("gtnhextranerf", "raw_machine_casing");
        check(rawBlock != null, "Raw casing block is missing");
        List<ItemStack> variations = CarvingUtils.getChiselRegistry()
            .getItemsForChiseling(finished.get(1));
        return variations.stream()
            .filter(stack -> stack.getItem() == Item.getItemFromBlock(rawBlock))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Raw Chisel variation missing for " + finished));
    }

    private static ItemStack item(OrePrefixes prefix, Materials material, int amount) {
        ItemStack stack = GTOreDictUnificator.get(prefix, material, amount);
        check(stack != null, "Required material form is missing: " + prefix + material);
        return stack;
    }

    private static GTRecipe find(ItemStack... inputs) {
        return RecipeMaps.assemblerRecipes.findRecipeQuery()
            .items(inputs)
            .voltage(TierEU.RECIPE_LV)
            .find();
    }

    private static boolean produces(GTRecipe recipe, ItemStack raw) {
        if (recipe == null || recipe.mOutputs == null) return false;
        for (ItemStack output : recipe.mOutputs) if (isRaw(output, raw)) return true;
        return false;
    }

    private static boolean isRaw(ItemStack stack, ItemStack raw) {
        return stack != null && stack.isItemEqual(raw) && stack.stackSize == 1;
    }

    private static boolean producesCasing(GTRecipe recipe, ItemStack raw, ItemList finished) {
        if (recipe == null || recipe.mOutputs == null) return false;
        for (ItemStack output : recipe.mOutputs) if (isCasing(output, raw, finished)) return true;
        return false;
    }

    private static boolean isCasing(ItemStack stack, ItemStack raw, ItemList finished) {
        return stack != null && (stack.isItemEqual(raw) || finished.isStackEqual(stack));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
